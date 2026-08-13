package com.mimic.monstermod.entity.hitbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * ボーンに追従する部位当たり判定。
 * バニラのEnderDragonPartと同じ仕組み(ForgeのPartEntity + IForgeEntity#isMultipartEntity/getParts)。
 *
 * 【汎用化の理由】
 * 実体として出したモンスター(YatagarasuEntity)と、モンスターに変身したプレイヤーの
 * 両方で同じ部位判定を使うため、親エンティティとダメージ転送先を可変にしてある。
 *
 * 【重要】Forgeはエンティティがワールドに追加された瞬間(ServerLevel$EntityCallbacks#onTrackingStart)
 * にしかパーツを登録しない。そのためプレイヤー用は「ログイン時に固定数を確保しておき、
 * 非変身時は休眠(disable)させる」方式を取る。後から個数を増やしても判定として機能しない。
 */
public class BoneHitboxPart extends PartEntity<Entity> {

    @Nullable private YatagarasuHitboxProfile.PartConfig config;
    @Nullable private LivingEntity damageTarget;
    private boolean enabled = false;

    public BoneHitboxPart(Entity parent) {
        super(parent);
        this.noPhysics = true;
    }

    /** この部位を有効化し、どの設定でどのエンティティにダメージを流すかを指定する */
    public void activate(YatagarasuHitboxProfile.PartConfig config, LivingEntity damageTarget) {
        this.config = config;
        this.damageTarget = damageTarget;
        this.enabled = true;
    }

    /** 休眠させる(非変身時など)。判定にも当たらなくなる */
    public void deactivate() {
        this.enabled = false;
        this.config = null;
        this.damageTarget = null;
        // 判定に引っかからないよう、親の位置に潰した箱を置いておく
        Entity parent = getParent();
        this.setPos(parent.getX(), parent.getY(), parent.getZ());
        this.setBoundingBox(new AABB(parent.getX(), parent.getY(), parent.getZ(),
                parent.getX(), parent.getY(), parent.getZ()));
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public YatagarasuHitboxProfile.PartConfig getConfig() {
        return config;
    }

    /** 計算済みのワールドAABBに合わせて位置・サイズを更新する */
    public void updateFromWorldAABB(AABB box) {
        this.setPos(box.getCenter().x, box.minY, box.getCenter().z);
        this.setBoundingBox(box);
    }

    @Override
    public boolean isPickable() {
        if (!enabled || damageTarget == null || !damageTarget.isAlive()) return false;

        // 【重要】自分のクライアントでは、自分自身のパーツを照準の対象にしない。
        // Level#getEntities(除外Entity, AABB, 条件) はパーツも結果に含めるが、
        // 除外されるのは本体だけでそのパーツは除外されない。そのため変身中は
        // 自分を取り囲む自分のパーツを常に照準が拾ってしまい、
        // 他のエンティティを殴れない・ブロックを置けない状態になる。
        //
        // isLocalPlayer() はサーバー側では常にfalseなので、サーバーの判定
        // (他プレイヤーからの攻撃)には影響しない。
        // 他プレイヤーのパーツも damageTarget が自分ではないため通常通り狙える。
        if (damageTarget instanceof Player p && p.isLocalPlayer()) return false;

        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        if (!enabled || config == null || damageTarget == null || !damageTarget.isAlive()) return false;
        // 自分の部位を自分で殴れてしまわないようにする(変身プレイヤー用)
        if (source.getEntity() == damageTarget) return false;

        boolean hurt = damageTarget.hurt(source, amount * config.damageMultiplier());
        if (hurt && config.hitSound() != null) {
            this.level().playSound(null, damageTarget.blockPosition(), config.hitSound(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return hurt;
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
