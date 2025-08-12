/*
package com.mimic.monster.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

public class ModProjectile extends Projectile {

    // 同期用のEntityDataパラメータ定義（ダメージ、寿命ティック数、ホーミング、発火状態、液体通過フラグ）
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(ModProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TICK = SynchedEntityData.defineId(ModProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOMING = SynchedEntityData.defineId(ModProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FIRE = SynchedEntityData.defineId(ModProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIQUID = SynchedEntityData.defineId(ModProjectile.class, EntityDataSerializers.BOOLEAN);

    protected boolean inGround;
    @Nullable
    private BlockState lastState;

    protected ModProjectile(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        // EntityDataの初期化
        this.entityData.define(DAMAGE, 0.0f);
        this.entityData.define(TICK, 0);
        this.entityData.define(HOMING, false);
        this.entityData.define(FIRE, false);
        this.entityData.define(LIQUID, false);
    }

    // ダメージ取得・設定
    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public void setDamage(float value) {
        this.entityData.set(DAMAGE, value);
    }

    // ティック数取得・設定
    public int getTick() {
        return this.entityData.get(TICK);
    }

    public void setTick(int value) {
        this.entityData.set(TICK, value);
    }

    // ホーミングフラグ
    public boolean isHoming() {
        return this.entityData.get(HOMING);
    }

    public void setHoming(boolean value) {
        this.entityData.set(HOMING, value);
    }

    // 発火フラグ
    public boolean isFire() {
        return this.entityData.get(FIRE);
    }

    public void setFire(boolean value) {
        this.entityData.set(FIRE, value);
    }

    // 液体通過フラグ
    public boolean isThroughLiquid() {
        return this.entityData.get(LIQUID);
    }

    public void setThroughLiquid(boolean value) {
        this.entityData.set(LIQUID, value);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setTick(compound.getInt("MHC_Tick"));
        this.setDamage(compound.getFloat("MHC_Damage"));
        this.setHoming(compound.getBoolean("MHC_Homing"));
        this.setFire(compound.getBoolean("MHC_Fire"));
        this.setThroughLiquid(compound.getBoolean("MHC_Liquid"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("MHC_Tick", this.getTick());
        compound.putFloat("MHC_Damage", this.getDamage());
        compound.putBoolean("MHC_Homing", this.isHoming());
        compound.putBoolean("MHC_Fire", this.isFire());
        compound.putBoolean("MHC_Liquid", this.isThroughLiquid());
    }

    @Override
    public void move(MoverType moverType, Vec3 movement) {
        super.move(moverType, movement);
        if (moverType != MoverType.SELF && this.shouldFall()) {
            this.startFalling();
        }
    }

    private boolean shouldFall() {
        // 地面にいるかつ周囲が空気や歩行可能なブロックかどうか
        return this.inGround && this.level.noCollision(new AABB(this.position(), this.position()).inflate(0.06));
    }

    private void startFalling() {
        this.inGround = false;
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(velocity.add(
                (this.random.nextFloat() * 0.2F),
                (this.random.nextFloat() * 0.2F),
                (this.random.nextFloat() * 0.2F)
        ));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (entity != this.getOwner()) {
            float damageAmount = this.getDamage() / 2.0F;
            Entity owner = this.getOwner() == null ? this : this.getOwner();
            DamageSource damageSource = this.level.damageSources().mobAttack(owner);
            if (this.isFire()) {
                // 独自の火炎ダメージソースを使う場合はここで変更
                // 例：damageSource = MHCDamageTypes.inFire(this, owner);
            }

            if (entity.hurt(damageSource, damageAmount)) {
                if (this.isFire()) {
                    entity.setSecondsOnFire(4);
                }

                // 一部の派生クラスは消滅しない想定（例外処理）
                if (!this.isRemoved() && !(this instanceof Ab_Nagant_Ammo) && !(this instanceof Ab_KuroMuchi) && !(this instanceof Ab_HeatRay)) {
                    this.discard(); // エンティティ削除
                }
            }
        }
    }

    @Override
    public void tick() {
        if (this.level.dimension() != ModDimension.COMPRESS) {  // 独自ディメンションなら特別処理
            super.tick();

            boolean wasInGround = this.isInGround();
            Vec3 motion = this.getDeltaMovement();

            if (this.xRot == 0.0F && this.yRot == 0.0F) {
                double horizMotion = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                this.setYRot((float) (Math.atan2(motion.x, motion.z) * (180F / Math.PI)));
                this.setXRot((float) (Math.atan2(motion.y, horizMotion) * (180F / Math.PI)));
                this.xRotO = this.getXRot();
                this.yRotO = this.getYRot();
            }

            BlockPos pos = this.blockPosition();
            BlockState blockState = this.level.getBlockState(pos);

            if (this instanceof Ab_Feather) {
                Ab_Feather feather = (Ab_Feather) this;
                if (feather.getOwner() != null && this.getOwner().isOnFire() && blockState.getBlock() instanceof LeavesBlock) {
                    // 特殊判定：羽の時のみ特定条件で処理スキップ
                }
            }

            if (!blockState.isAir() && !wasInGround) {
                VoxelShape shape = blockState.getCollisionShape(this.level, pos);
                if (!shape.isEmpty()) {
                    Vec3 positionVec = this.position();

                    for (AABB aabb : shape.toAabbs()) {
                        if (aabb.move(pos).contains(positionVec)) {
                            this.inGround = true;
                            break;
                        }
                    }
                }
            }

            if (this.inGround && !wasInGround) {
                if (this.lastState != blockState && this.shouldFall()) {
                    this.startFalling();
                }
            } else {
                Vec3 oldPos = this.position();
                HitResult hitResult = ProjectileUtil.getHitResult(this, this::canHitEntity);

                if (hitResult.getType() != Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hitResult)) {
                    this.onHit(hitResult);
                }

                motion = this.getDeltaMovement();

                double dx = motion.x;
                double dy = motion.y;
                double dz = motion.z;

                double newX = oldPos.x + dx;
                double newY = oldPos.y + dy;
                double newZ = oldPos.z + dz;

                double horizSpeed = Math.sqrt(dx * dx + dz * dz);

                if (wasInGround) {
                    this.setYRot((float) (Math.atan2(-dx, -dz) * (180F / Math.PI)));
                } else {
                    this.setYRot((float) (Math.atan2(dx, dz) * (180F / Math.PI)));
                }

                this.setXRot((float) (Math.atan2(dy, horizSpeed) * (180F / Math.PI)));
                this.setXRot(clampRotation(this.xRotO, this.getXRot()));
                this.setYRot(clampRotation(this.yRotO, this.getYRot()));

                this.setPos(newX, newY, newZ);
                this.refreshDimensions();

                float drag = 0.99F;
                float gravity = 0.05F;

                if (this.isInWater()) {
                    for (int i = 0; i < 4; i++) {
                        this.level.addParticle(ParticleTypes.BUBBLE, newX - dx * 0.25, newY - dy * 0.25, newZ - dz * 0.25, dx, dy, dz);
                    }
                    if (!this.isThroughLiquid()) {
                        drag = 0.6F;
                    }
                }

                this.setDeltaMovement(motion.scale(drag));

                if (!this.isNoGravity() && !wasInGround) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -gravity, 0.0D));
                }

                this.setPos(newX, newY, newZ);
            }
        }
    }

    @Override
    public boolean isPickable() {
        // 表示距離判定（描画距離？）カスタム可
        double d0 = this.getBoundingBox().getSize() * 10.0F;
        if (Double.isNaN(d0)) {
            d0 = 1.0F;
        }
        d0 *= 64.0F * getViewScale();
        return super.isPickable();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        this.lastState = this.level.getBlockState(result.getBlockPos());
        super.onHitBlock(result);

        Vec3 hitVec = result.getLocation().subtract(this.getX(), this.getY(), this.getZ());

        if (!(this instanceof Ab_Onpa) && !(this instanceof Ab_Nagant_Ammo) && !(this instanceof Ab_HeatRay)) {
            this.setDeltaMovement(hitVec);
            Vec3 motion = hitVec.normalize().scale(0.05F);
            this.setPos(this.getX() - motion.x, this.getY() - motion.y, this.getZ() - motion.z);
            this.inGround = true;
        }
    }

    // 回転補間用（元m_37273_）
    private static float clampRotation(float prev, float next) {
        float f = Mth.wrapDegrees(next - prev);
        return prev + f * 0.2F;
    }
}
*/