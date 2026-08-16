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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

/**
 * 竜巻(渦)の見た目だけを受け持つエンティティ。
 *
 * 【既存の TornadoEntity と分けている理由】
 * TornadoEntity は吸い上げ・打ち上げ・ダメージといった挙動を持つ「仕掛け」側。
 * こちらは見た目専用で、挙動は一切持たない。
 * 分けておくと、見た目を作り込んでも挙動が壊れず、
 * 逆に既存の竜巻の見た目だけを差し替えることもできる。
 *
 * 【形はコードで組み立てる】
 * 参考画像のような「大きさの違う光の帯が何重にも巻き付いた渦」は、
 * Blockbenchで層を1枚ずつ作るのは現実的ではない。
 * 帯の枚数・巻き数・太さを数値で持たせて
 * {@link com.mimic.monstermod.entity.render.VortexRenderer} 側で毎フレーム組み立てる。
 */
public class VortexEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TOP_RADIUS =
            SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BOTTOM_RADIUS =
            SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.INT);

    /** 出現時と消滅時のフェード(tick) */
    public static final int FADE_TICKS = 10;

    public VortexEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true; // 背が高いので、足元が画面外でも描画を切られないようにする
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEIGHT, 6.0F);
        this.entityData.define(DATA_TOP_RADIUS, 2.6F);
        this.entityData.define(DATA_BOTTOM_RADIUS, 0.45F);
        this.entityData.define(DATA_COLOR, 0xBFE8FF); // 参考画像に近い白〜水色
        this.entityData.define(DATA_LIFE, 600);
    }

    /** 形と寿命をまとめて設定する */
    public void configure(float height, float bottomRadius, float topRadius, int life) {
        this.entityData.set(DATA_HEIGHT, height);
        this.entityData.set(DATA_BOTTOM_RADIUS, bottomRadius);
        this.entityData.set(DATA_TOP_RADIUS, topRadius);
        this.entityData.set(DATA_LIFE, life);
    }

    public void setColor(int rgb) { this.entityData.set(DATA_COLOR, rgb); }

    public float getVortexHeight()  { return this.entityData.get(DATA_HEIGHT); }
    public float getTopRadius()     { return this.entityData.get(DATA_TOP_RADIUS); }
    public float getBottomRadius()  { return this.entityData.get(DATA_BOTTOM_RADIUS); }
    public int   getColor()         { return this.entityData.get(DATA_COLOR); }
    public int   getLife()          { return this.entityData.get(DATA_LIFE); }

    /** 出始めと終わりで薄くするための係数(0〜1) */
    public float getFade(float partialTick) {
        float t = this.tickCount + partialTick;
        float in = Mth.clamp(t / FADE_TICKS, 0.0F, 1.0F);
        float out = Mth.clamp((getLife() - t) / FADE_TICKS, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    @Override
    public void tick() {
        super.tick();
        // 見た目だけの存在なので、寿命の管理以外は何もしない
        if (!this.level().isClientSide && this.tickCount > getLife()) {
            this.discard();
        }
    }

    /** 上に伸びる形なので、当たり判定の箱も上方向へ広げておく(描画の判定に使われる) */
    @Override
    public AABB getBoundingBoxForCulling() {
        float r = getTopRadius() + 1.0F;
        return new AABB(getX() - r, getY(), getZ() - r,
                getX() + r, getY() + getVortexHeight() + 1.0F, getZ() + r);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 192.0D * 192.0D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // 一時的なエフェクトなのでワールドには保存しない
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
