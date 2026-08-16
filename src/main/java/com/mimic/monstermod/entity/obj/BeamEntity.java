package com.mimic.monstermod.entity.obj;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * まっすぐ伸びる光のビーム。
 *
 * 【役割分担】
 *  サーバー: 射手への追従・ブロックまでの距離の計算・当たり判定とダメージ
 *  クライアント: 見た目の描画のみ({@link com.mimic.monstermod.entity.render.BeamRenderer})
 *
 * 【射手に追従する仕組み】
 * 射手のIDだけを同期しておき、原点と向きはクライアント側でも射手から計算し直す。
 * ビームの座標をそのまま同期するとカクつく(サーバーは1tickに1回しか送らない)が、
 * この方式なら射手の補間済みの視点から毎フレーム引き直せるので、
 * 視点を動かしてもビームがぴったり追従する。
 *
 * 【長さの決まり方】
 * 射線上のブロックに当たるまでの距離をサーバーで測って同期する。
 * これにより壁の向こうへビームが突き抜けて見えることがない。
 * エンティティは貫通する(1体で止まらない)。
 */
public class BeamEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);
    // 消えるタイミングをクライアントでも知る必要がある(終わりのフェードに使うため)
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.INT);

    /** 見た目が消えるまでの立ち上がり/終わりのフェード(tick) */
    public static final int FADE_TICKS = 4;

    private LivingEntity owner;
    private float maxLength = 48.0F;
    private float damage = 4.0F;
    private int damageInterval = 4;

    public BeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true; // 長いので、原点が画面外でも描画を切られないようにする
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER, -1);
        this.entityData.define(DATA_LENGTH, 0.0F);
        this.entityData.define(DATA_RADIUS, 0.35F);
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 0.0F);
        this.entityData.define(DATA_COLOR, 0xFFB020); // 既定は画像のような黄橙色
        this.entityData.define(DATA_LIFE, 40);
    }

    // ---------------- 設定 ----------------

    /** 射手に追従するビームとして撃つ */
    public void fireFrom(LivingEntity shooter, float maxLength, float radius, float damage,
                         int damageInterval, int life) {
        this.owner = shooter;
        this.maxLength = maxLength;
        this.damage = damage;
        this.damageInterval = damageInterval;

        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_OWNER, shooter.getId());
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_LENGTH, maxLength);
        syncRotationFrom(shooter.getYRot(), shooter.getXRot());
        this.setPos(muzzleOf(shooter, 1.0F));
    }

    /**
     * 射手に追従しない、その場に固定されたビームとして置く。
     * 撃っている様子を自分で歩き回って好きな角度から見たいとき用。
     * 射手がいないので、向きは同期した yaw/pitch から決まる。
     */
    public void placeStatic(Vec3 pos, float yaw, float pitch, float maxLength, float radius,
                            float damage, int damageInterval, int life) {
        this.owner = null;
        this.maxLength = maxLength;
        this.damage = damage;
        this.damageInterval = damageInterval;

        this.entityData.set(DATA_OWNER, -1);
        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_LENGTH, maxLength);
        syncRotationFrom(yaw, pitch);
        this.setPos(pos);
    }

    public void setColor(int rgb) { this.entityData.set(DATA_COLOR, rgb); }
    public void setLife(int ticks) { this.entityData.set(DATA_LIFE, ticks); }

    private void syncRotationFrom(float yaw, float pitch) {
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    // ---------------- 見た目のための値(クライアントからも呼ぶ) ----------------

    public float getRadius() { return this.entityData.get(DATA_RADIUS); }
    public float getLength() { return this.entityData.get(DATA_LENGTH); }
    public int getColor()    { return this.entityData.get(DATA_COLOR); }
    public int getLife()     { return this.entityData.get(DATA_LIFE); }

    @Nullable
    public Entity getOwnerEntity() {
        int id = this.entityData.get(DATA_OWNER);
        return id < 0 ? null : this.level().getEntity(id);
    }

    /** ビームの出る位置。射手がいれば視点の少し前(手元あたり)から出す */
    public Vec3 getBeamOrigin(float partialTick) {
        Entity o = getOwnerEntity();
        if (o != null) return muzzleOf(o, partialTick);

        return new Vec3(
                Mth.lerp(partialTick, this.xo, this.getX()),
                Mth.lerp(partialTick, this.yo, this.getY()),
                Mth.lerp(partialTick, this.zo, this.getZ()));
    }

    /** 進行方向(正規化済み) */
    public Vec3 getBeamDirection(float partialTick) {
        Entity o = getOwnerEntity();
        if (o != null) return o.getViewVector(partialTick);
        return Vec3.directionFromRotation(this.entityData.get(DATA_PITCH), this.entityData.get(DATA_YAW));
    }

    /** 射手の視点から少し前に出した「銃口」の位置 */
    private static Vec3 muzzleOf(Entity shooter, float partialTick) {
        Vec3 eye = shooter.getEyePosition(partialTick);
        Vec3 look = shooter.getViewVector(partialTick);
        return eye.add(look.scale(0.6)).add(0.0, -0.15, 0.0);
    }

    /** 撃ち始めと撃ち終わりで細く/薄くするための係数(0〜1) */
    public float getFade(float partialTick) {
        float t = this.tickCount + partialTick;
        float in = Mth.clamp(t / FADE_TICKS, 0.0F, 1.0F);
        float out = Mth.clamp((getLife() - t) / FADE_TICKS, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    // ---------------- 処理 ----------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return; // 見た目だけなのでクライアントでは何もしない

        Entity o = getOwnerEntity();
        if (this.tickCount > getLife() || (o != null && !o.isAlive())) {
            this.discard();
            return;
        }

        // 射手に追従させる(位置を合わせないとクライアントの追跡範囲から外れる)
        if (o != null) {
            this.setPos(muzzleOf(o, 1.0F));
            syncRotationFrom(o.getYRot(), o.getXRot());
        }

        Vec3 origin = getBeamOrigin(1.0F);
        Vec3 dir = getBeamDirection(1.0F);

        float reach = clipToBlocks(origin, dir);
        this.entityData.set(DATA_LENGTH, reach);

        if (this.damage > 0 && this.tickCount % this.damageInterval == 0) {
            hitEntities(origin, dir, reach);
        }
    }

    /** ブロックに当たるまでの距離を測る。当たらなければ最大長 */
    private float clipToBlocks(Vec3 origin, Vec3 dir) {
        Vec3 end = origin.add(dir.scale(this.maxLength));
        BlockHitResult hit = this.level().clip(new ClipContext(
                origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() == HitResult.Type.MISS) return this.maxLength;
        return (float) hit.getLocation().distanceTo(origin);
    }

    /**
     * 射線上のエンティティにダメージを与える。
     * 1体で止まらず貫通する(ビームらしさを優先)。
     */
    private void hitEntities(Vec3 origin, Vec3 dir, float reach) {
        Vec3 end = origin.add(dir.scale(reach));
        double r = getRadius();

        AABB area = new AABB(origin, end).inflate(r + 0.3);
        List<Entity> candidates = this.level().getEntities(this, area,
                e -> e != this.owner && e != getOwnerEntity() && e.isAlive() && e.isPickable());

        for (Entity target : candidates) {
            // 太さぶん膨らませた当たり判定を射線で切る
            if (target.getBoundingBox().inflate(r).clip(origin, end).isEmpty()) continue;
            target.hurt(this.damageSources().indirectMagic(this, this.owner), this.damage);
        }
    }

    // ---------------- その他 ----------------

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 256.0D * 256.0D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // 一時的なエフェクトなのでワールドには保存しない
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
