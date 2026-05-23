package com.mimic.monstermod.entity.obj;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpiralOnibiEntity extends OnibiEntity {
    private Vec3 centerPos = Vec3.ZERO;
    private double radius = 0.0;
    private double angle = 0.0;
    private double speed = 0.21;
    private double rotationSpeed = 0.0;

    public SpiralOnibiEntity(EntityType<? extends SpiralOnibiEntity> type, Level level) {
        super(type, level);
    }

    public void setSpiralProperties(Vec3 center, double startAngle, double speed, double rotation, int life) {
        this.centerPos = center;
        this.angle = startAngle;
        this.speed = speed;
        this.rotationSpeed = rotation;
        // 親のプロパティを設定（速度は螺旋計算で上書きするのでZEROでOK）
        this.setProperties(life, true, true, Vec3.ZERO);
    }

    @Override
    public void tick() {
        // 現在の座標を保持
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos;

        if (!this.level().isClientSide && centerPos != Vec3.ZERO) {
            // --- 1. 螺旋の座標計算 ---
            this.angle += rotationSpeed;
            this.radius += speed;

            double nextX = centerPos.x + Math.cos(angle) * radius;
            double nextZ = centerPos.z + Math.sin(angle) * radius;
            double nextY = centerPos.y;

            nextPos = new Vec3(nextX, nextY, nextZ);

            // 衝突判定用に「今このtickで動いたベクトル」を算出
            Vec3 motion = nextPos.subtract(currentPos);

            // --- 2. ブロック衝突判定 ---
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onHitBlock(blockHit);
                // ブロックに当たった場合はその地点で止まる（discardされない設定なら）
                nextPos = blockHit.getLocation();
            }

            // --- 3. エンティティ衝突判定 ---
            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    this.level(), this, currentPos, nextPos,
                    this.getBoundingBox().expandTowards(motion).inflate(1.0D),
                    this::canHitEntity);

            if (entityHit != null) {
                this.onHitEntity(entityHit);
            }
        }

        // 座標を確定
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // 寿命チェック
        if (this.tickCount > 600) {
            this.discard();
        }
        this.tickCount++;
    }
}