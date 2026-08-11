package com.mimic.monstermod.entity.hitbox;

import com.mimic.monstermod.entity.monster.YatagarasuEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

/**
 * Yatagarasuの部位(頭・翼・尻尾など)ごとの当たり判定。
 * バニラのEnderDragonPart(EnderDragon)と全く同じ仕組み(Forgeが提供するPartEntity +
 * IForgeEntity#isMultipartEntity/getParts)を使う。通常のEntityType登録や
 * level.addFreshEntity()は行わない(EnderDragonPart同様、getAddEntityPacket()は
 * 使われないため素直にUnsupportedOperationExceptionのままにしておく。
 * オーナー側のisMultipartEntity()/getParts()経由でForgeが同期・ピック対象に含めてくれる)。
 * 物理的な衝突(壁になる)は持たない。ダメージは本体(YatagarasuEntity)へ委譲する。
 */
public class YatagarasuBodyPart extends PartEntity<YatagarasuEntity> {

    private final YatagarasuHitboxProfile.PartConfig config;

    public YatagarasuBodyPart(YatagarasuEntity parent, YatagarasuHitboxProfile.PartConfig config) {
        super(parent);
        this.config = config;
        this.noPhysics = true;
    }

    public YatagarasuHitboxProfile.PartConfig getConfig() {
        return config;
    }

    /** サーバー側で毎tick、計算済みのワールドAABBに合わせて位置・サイズを更新する */
    public void updateFromWorldAABB(AABB box) {
        this.setPos(box.getCenter().x, box.minY, box.getCenter().z);
        this.setBoundingBox(box);
    }

    @Override
    public boolean isPickable() {
        return getParent().isAlive();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || !getParent().isAlive()) return false;

        boolean hurt = getParent().hurt(source, amount * config.damageMultiplier());
        if (hurt && config.hitSound() != null) {
            this.level().playSound(null, getParent().blockPosition(), config.hitSound(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return hurt;
    }

    @Override
    protected void defineSynchedData() {
        // 独自の同期データは持たない(親エンティティ側で管理する)
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 保存しない(親から毎tick再生成される計算専用エンティティのため)
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 保存しない
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
