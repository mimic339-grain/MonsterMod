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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * electricity(電撃)の本体。始点から終点へ走る稲妻。
 *
 * 【スプライトではなくコードで形を作る理由】
 * 稲妻は「A地点からB地点へジグザグに走る経路」であって、決まった絵ではない。
 * 板に絵を貼る方式では始点と終点を結べず、枝分かれもできず、
 * 同じ形が何度も出て繰り返しに見える。
 * 経路そのものを作れば、狙った相手まで確実に届き、毎回違う形になる。
 *
 * 【形を同期しない理由】
 * 折れ曲がりの座標を全部送ると量が多い。
 * 代わりに種(seed)だけを同期し、同じ計算をクライアントでも行っている。
 * 種と時間が同じなら誰の画面でも同じ形になるので、見た目がずれない。
 */
public class LightningBoltEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_END_X =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Y =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Z =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_THICKNESS =
            SynchedEntityData.defineId(LightningBoltEntity.class, EntityDataSerializers.FLOAT);

    /** 何tickごとに形を作り直すか。短いほど激しくちらつく */
    public static final int FLICKER_TICKS = 2;

    private LivingEntity owner;
    private float damage;
    /** 電撃の当たる太さ。見た目の細さより少し広く取らないと当たらない */
    private static final double HIT_RADIUS = 1.6;

    public LightningBoltEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true; // 長いので、始点が画面外でも描画を切られないようにする
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER, -1);
        this.entityData.define(DATA_END_X, 0.0F);
        this.entityData.define(DATA_END_Y, 0.0F);
        this.entityData.define(DATA_END_Z, 0.0F);
        this.entityData.define(DATA_LIFE, 12);
        this.entityData.define(DATA_SEED, 0);
        this.entityData.define(DATA_COLOR, 0x3060FF); // 参考画像に近い青
        this.entityData.define(DATA_THICKNESS, 0.18F);
    }

    // ---------------- 設定 ----------------

    /**
     * 始点から終点へ走る電撃を作る。
     * 終点は始点からの相対位置で持たせるので、遠くても精度が落ちない。
     */
    public void setup(LivingEntity owner, Vec3 start, Vec3 end,
                      float damage, float thickness, int life) {
        this.owner = owner;
        this.damage = damage;

        this.setPos(start);
        Vec3 rel = end.subtract(start);
        this.entityData.set(DATA_END_X, (float) rel.x);
        this.entityData.set(DATA_END_Y, (float) rel.y);
        this.entityData.set(DATA_END_Z, (float) rel.z);
        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_THICKNESS, thickness);
        this.entityData.set(DATA_SEED, this.random.nextInt());
        this.entityData.set(DATA_OWNER, owner == null ? -1 : owner.getId());
    }

    public void setColor(int rgb) { this.entityData.set(DATA_COLOR, rgb); }

    // ---------------- 見た目のための値 ----------------

    /** 終点(始点からの相対位置) */
    public Vec3 getRelativeEnd() {
        return new Vec3(this.entityData.get(DATA_END_X),
                this.entityData.get(DATA_END_Y),
                this.entityData.get(DATA_END_Z));
    }

    public int getLife()        { return this.entityData.get(DATA_LIFE); }
    public int getSeed()        { return this.entityData.get(DATA_SEED); }
    public int getColor()       { return this.entityData.get(DATA_COLOR); }
    public float getThickness() { return this.entityData.get(DATA_THICKNESS); }

    /**
     * 明るさの移り変わり。
     * 出た瞬間が一番強く、あとは尾を引くように弱まりながら細かく明滅する。
     * 稲妻は一定の明るさで光り続けないので、これがないと蛍光灯のように見える。
     */
    public float getIntensity(float partialTick) {
        float t = (this.tickCount + partialTick) / Math.max(1, getLife());
        if (t >= 1.0F) return 0.0F;

        float decay = (1.0F - t) * (1.0F - t);
        // 明滅。完全には消さず、下限を残して途切れないようにする
        float flicker = 0.65F + 0.35F * Mth.sin((this.tickCount + partialTick) * 2.7F);
        return decay * flicker;
    }

    // ---------------- 処理 ----------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (this.tickCount > getLife()) {
            this.discard();
            return;
        }

        // 走った瞬間に一度だけ効かせる。毎tick入れると即死してしまう
        if (this.tickCount == 1 && this.damage > 0) {
            zapAlongPath();
        }
    }

    /**
     * 始点と終点を結ぶ線の近くにいる相手を撃つ。
     *
     * 見た目はジグザグに走るが、当たり判定はまっすぐな線で取っている。
     * 折れ曲がりに沿って判定すると「見た目は当たっているのに当たらない」が起きやすく、
     * 太さに余裕を持たせた直線のほうが結果が素直になる。
     */
    private void zapAlongPath() {
        Vec3 start = this.position();
        Vec3 end = start.add(getRelativeEnd());

        AABB area = new AABB(start, end).inflate(HIT_RADIUS + 0.5);
        List<Entity> targets = this.level().getEntities(this, area,
                e -> e != this.owner && e.isAlive() && e.isPickable());

        for (Entity target : targets) {
            if (target.getBoundingBox().inflate(HIT_RADIUS).clip(start, end).isEmpty()) continue;
            target.hurt(this.damageSources().indirectMagic(this, this.owner), this.damage);
        }
    }

    /** 稲妻は長く伸びるので、始点だけでなく終点まで含めて描画対象にする */
    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 rel = getRelativeEnd();
        AABB line = new AABB(getX(), getY(), getZ(),
                getX() + rel.x, getY() + rel.y, getZ() + rel.z);
        // 枝分かれが本線からはみ出すぶんの余裕
        return line.inflate(6.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 192.0D * 192.0D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // 一瞬で消えるエフェクトなのでワールドには保存しない
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
