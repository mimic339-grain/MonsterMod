package com.mimic.monstermod.entity.obj;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class BigTornadoEntity extends Entity {
    // データIDには自身のクラスを指定する
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BigTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BigTornadoEntity.class, EntityDataSerializers.INT);

    private LivingEntity owner;
    private float damage;

    public BigTornadoEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SIZE, 4.5f); // 3.0 * 1.5
        this.entityData.define(DATA_LIFE, 200);
    }

    public void setProperties(LivingEntity owner, int life, float baseSize, float baseDamage) {
        this.owner = owner;
        this.damage = baseDamage * 1.5f; // ダメージ1.5倍
        this.entityData.set(DATA_SIZE, baseSize * 1.5f); // サイズ1.5倍
        this.entityData.set(DATA_LIFE, life);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.tickCount > this.entityData.get(DATA_LIFE) || (owner != null && !owner.isAlive())) {
                this.discard();
                return;
            }
            applyStormEffect(this.entityData.get(DATA_SIZE));
        }
    }

    private void applyStormEffect(float radius) {
        float height = 45.0f; // 30.0f * 1.5
        float pullRange = radius + 3.75f; // 2.5f * 1.5
        AABB hitArea = this.getBoundingBox().inflate(radius + 10.5, height + 15.0, radius + 10.5);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitArea, e -> e != owner && e.isAlive());

        for (LivingEntity target : targets) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dy = target.getY() - this.getY();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            float currentRadiusAtHeight = calculateRadiusAtHeight((float)dy);

            if (horizontalDist <= currentRadiusAtHeight + 3.75f) { // 2.5 * 1.5
                if (this.tickCount % 5 == 0) {
                    target.hurt(this.damageSources().magic(), this.damage);
                }

                Vec3 currentVec = target.getDeltaMovement();

                // 1.5倍スケールに合わせた物理演算
                if (dy < height - 3.0f) {
                    // フェーズA: 吸い込み・回転・上昇
                    Vec3 toCenter = new Vec3(dx, 0, dz).normalize();
                    double pullFactor = 0.15 * 1.5; // 吸い込みも強化
                    Vec3 pull = toCenter.scale(-pullFactor);
                    double rotateSpeed = 0.5 * 1.5; // 回転も少し速く
                    Vec3 tangent = new Vec3(-dz, 0, dx).normalize().scale(rotateSpeed);
                    double liftSpeed = 0.5 * 1.5; // 上昇速度1.5倍

                    target.setDeltaMovement(
                            currentVec.x * 0.5 + (pull.x + tangent.x),
                            liftSpeed,
                            currentVec.z * 0.5 + (pull.z + tangent.z)
                    );

                    if (target instanceof net.minecraft.world.entity.player.Player player) {
                        player.setYRot(player.getYRot() + 18.0f); // 12.0 * 1.5
                    }
                } else {
                    // フェーズB & C: 頂上での放出
                    double topRotateSpeed = 1.2 * 1.5;
                    target.setDeltaMovement((-dz / horizontalDist) * topRotateSpeed, 0.03, (dx / horizontalDist) * topRotateSpeed);

                    if (target.tickCount % 20 == 0) {
                        Vec3 launch = new Vec3(dx, 0, dz).normalize().scale(5 * 1.5).add(0, 1.5 * 1.5, 0);
                        target.setDeltaMovement(launch);
                        target.hurtMarked = true;
                        target.teleportTo(target.getX() + launch.x, this.getY() + height + 3.0, target.getZ() + launch.z);
                    }
                }
                target.hurtMarked = true;
                target.fallDistance = 0;
            }
        }
    }

    private float calculateRadiusAtHeight(float h) {
        float tornadoHeight = 45.0f; // 30 * 1.5
        if (h < 0) return 4.5f; // 3.0 * 1.5
        if (h > tornadoHeight) return 22.5f; // 15.0 * 1.5

        float hProgress = h / tornadoHeight;
        return (3.0f + ((float) Math.pow(hProgress, 4.0) * 12.0f)) * 1.5f;
    }
    public float getScale() {
        return this.entityData.get(DATA_SIZE);
    }
    public int getLifetime() {
        return this.entityData.get(DATA_LIFE);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}