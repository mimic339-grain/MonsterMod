package com.mimic.monstermod.entity.obj;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * メギド。黒い球とその周りの円盤(土星のような形)からなる大技。
 *
 * 【流れ】
 *  ためる(CHARGE) … 円盤の外側から黒い粒が球へ吸い込まれていく。
 *                    進むほど吸い込みが速くなり、球の中の輝きも増えていく。
 *  はじける(BANG) … 集まったものが一気に外へ放出される。
 *
 * 見た目の担当は {@link com.mimic.monstermod.entity.render.MegiddoRenderer}。
 * こちらは「今どの段階で、どれくらい進んでいるか」を持ってクライアントへ伝える役。
 * 進み具合(0〜1)さえ渡せば、粒の速さも輝きの量も描画側で決められる。
 */
public class MegiddoEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(MegiddoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE =
            SynchedEntityData.defineId(MegiddoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BLAST_RADIUS =
            SynchedEntityData.defineId(MegiddoEntity.class, EntityDataSerializers.INT);

    /**
     * はじけている時間。
     * 飛び散った光が粉雪のように落ちて消えるところまで見せたいので、長めに取っている。
     */
    public static final int BANG_TICKS = 90;

    private float damage = 40.0F;

    public MegiddoEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true; // 大きいので原点が画面外でも描画を切られないようにする
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 3.0F);        // 中心の黒い球の半径
        this.entityData.define(DATA_CHARGE, 200);         // ためる時間(tick)
        this.entityData.define(DATA_BLAST_RADIUS, 24);    // はじけたときに巻き込む範囲
    }

    /** 大きさと時間をまとめて決める */
    public void configure(float sphereRadius, int chargeTicks, int blastRadius, float damage) {
        this.entityData.set(DATA_RADIUS, sphereRadius);
        this.entityData.set(DATA_CHARGE, Math.max(20, chargeTicks));
        this.entityData.set(DATA_BLAST_RADIUS, Math.max(1, blastRadius));
        this.damage = damage;
    }

    public float getSphereRadius()  { return this.entityData.get(DATA_RADIUS); }
    public int getChargeTicks()     { return this.entityData.get(DATA_CHARGE); }
    public int getBlastRadius()     { return this.entityData.get(DATA_BLAST_RADIUS); }

    /** 円盤の半径。球より一回り大きくして土星のような見た目にする */
    public float getRingRadius() {
        return getSphereRadius() * 3.4F;
    }

    /**
     * 見た目の中心を、足元からどれだけ持ち上げるか。
     *
     * 円盤は傾いているぶん下へ張り出すので、低い位置に出すと地面に食い込んで
     * 輪が途中で切れて見えてしまう。それを避けるだけの高さを確保している。
     * 描画とダメージの中心がずれないよう、両方でこの値を使う。
     */
    public double getCenterOffset() {
        return getRingRadius() * 0.30 + getSphereRadius() + 1.0;
    }

    /** ための進み具合(0〜1)。1になった瞬間にはじける */
    public float getChargeProgress(float partialTick) {
        float t = this.tickCount + partialTick;
        return Mth.clamp(t / getChargeTicks(), 0.0F, 1.0F);
    }

    public boolean isBanging(float partialTick) {
        return this.tickCount + partialTick >= getChargeTicks();
    }

    /** はじけてからの進み具合(0〜1) */
    public float getBangProgress(float partialTick) {
        float t = this.tickCount + partialTick - getChargeTicks();
        return Mth.clamp(t / BANG_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        int charge = getChargeTicks();

        // ためている間は、吸い込みの音を少しずつ高くしていく
        if (this.tickCount < charge && this.tickCount % 10 == 0) {
            float progress = (float) this.tickCount / charge;
            this.level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE,
                    2.0F, 0.5F + progress * 1.2F);
        }

        if (this.tickCount == charge) {
            bigBang();
        }

        if (this.tickCount > charge + BANG_TICKS) {
            this.discard();
        }
    }

    /**
     * はじける瞬間。集めたものを一気に外へ放出する。
     * 見た目の派手さに対して当たり判定が小さいとちぐはぐなので、
     * 巻き込む範囲は描画で広がる大きさに合わせてある。
     */
    private void bigBang() {
        if (!(this.level() instanceof ServerLevel level)) return;

        int r = getBlastRadius();
        // 見た目の中心から飛び散るので、判定の中心も同じ高さに合わせる
        Vec3 center = this.position().add(0.0, getCenterOffset(), 0.0);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.5F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3.0F, 0.7F);

        AABB area = new AABB(center.x - r, center.y - r, center.z - r,
                center.x + r, center.y + r, center.z + r);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e.position().distanceToSqr(center) <= (double) r * r);

        for (LivingEntity target : targets) {
            // 中心ほど痛い。外周でも最低限は入る
            double dist = target.position().distanceTo(center);
            float falloff = 1.0F - (float) (dist / r) * 0.7F;
            target.hurt(level.damageSources().explosion(null, null), damage * falloff);

            // 外へ吹き飛ばす。放出されている見た目と動きを合わせる
            Vec3 push = target.position().subtract(center).normalize().scale(2.0);
            target.setDeltaMovement(push.x, Math.max(0.6, push.y), push.z);
            target.hurtMarked = true;
        }
    }

    /** 円盤や飛び散る光まで含めた大きさで描画対象にする */
    @Override
    public AABB getBoundingBoxForCulling() {
        double r = Math.max(getRingRadius(), getBlastRadius() * 1.6) + 8.0;
        double cy = getY() + getCenterOffset();
        return new AABB(getX() - r, cy - r, getZ() - r, getX() + r, cy + r, getZ() + r);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 256.0D * 256.0D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // 一時的な演出なのでワールドには保存しない
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
