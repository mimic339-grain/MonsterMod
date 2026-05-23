package com.mimic.monstermod.entity.obj;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class OnibiEntity extends AbstractArrow {
    private int maxLife = 400;
    private boolean pierceEntity = false;
    private boolean hitBlockDestroy = true;

    // 当たり判定の枠を表示するかどうかのフラグ
    public boolean showDebugHitbox = false;

    public OnibiEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (hitBlockDestroy) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 1. ダメージと炎上を与える
        result.getEntity().hurt(this.damageSources().magic(), 6.0F);
        result.getEntity().setSecondsOnFire(5);
    }

    // setProperties も常に貫通するように上書きしておくと安全です
    public void setProperties(int life, boolean pierce, boolean blockDestroy, Vec3 velocity) {
        this.maxLife = life;
        this.pierceEntity = true; // 常に突き抜けるように強制
        this.hitBlockDestroy = blockDestroy;
        this.setDeltaMovement(velocity);
        this.setPierceLevel((byte) 5); // 貫通レベルを高く設定
    }

    @Override
    public void tick() {
        // --- 移動処理 ---
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);

        if (!this.level().isClientSide) {
            // --- A. ブロック衝突判定 ---
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onHitBlock(blockHit);
                nextPos = blockHit.getLocation(); // 衝突地点で止める
            }

            // --- B. エンティティ衝突判定 (追加) ---
            // これを入れないと、弾がプレイヤーを通り抜けてしまいます
            EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    this.level(), this, currentPos, nextPos,
                    this.getBoundingBox().expandTowards(motion).inflate(1.0D),
                    this::canHitEntity);

            if (entityHit != null) {
                this.onHitEntity(entityHit);
            }
        }

        // 位置の更新
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // 寿命チェック
        if (this.tickCount > maxLife) {
            this.discard();
        }
        this.tickCount++;
    }

    @Override
    protected ItemStack getPickupItem() { return ItemStack.EMPTY; }
}