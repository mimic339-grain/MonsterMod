// MimicEntity.java（Pro_AllMight準拠完全版）
package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * MimicEntity（Pro_AllMightスタイル）
 * - predicate() で動的にアニメーションを制御
 * - スキル・開閉・移動状態に応じて再生するアニメーションを変更
 */
public class MimicEntity extends BaseMonsterEntity {
    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> BITE =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public MimicEntity(EntityType<? extends BaseMonsterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
        this.entityData.define(BITE, false);
    }

    public boolean isOpen() { return this.entityData.get(OPEN); }
    public void setOpen(boolean open) { this.entityData.set(OPEN, open); }

    public boolean isBiting() { return this.entityData.get(BITE); }
    public void setBiting(boolean bite) { this.entityData.set(BITE, bite); }

    // ==============================================================
    // GeckoLib Animation Controller
    // ==============================================================
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
        controllers.add(new AnimationController<>(this, "mouthcontroller", 0, this::mouthPredicate));
    }

    /**
     * メインアニメーション制御
     */
    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> event) {
        AnimationController<T> controller = event.getController();
        IMonsterData data = getMonsterData();

        // 1. スキル発動中
        if (data != null && data.getSkill() != null && !data.getSkill().isEmpty()) {
            String skill = data.getSkill();
            controller.setAnimation(RawAnimation.begin().then("animation.mimic.skill_" + skill, Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        // 2. 通常行動
        if (event.isMoving()) {
            if (isOpen()) {
                controller.setAnimation(RawAnimation.begin().then("animation.mimic.open_walk", Animation.LoopType.LOOP));
            } else {
                controller.setAnimation(RawAnimation.begin().then("animation.mimic.close_walk", Animation.LoopType.LOOP));
            }
        } else {
            if (isOpen()) {
                controller.setAnimation(RawAnimation.begin().then("animation.mimic.open_idle", Animation.LoopType.LOOP));
            } else {
                controller.setAnimation(RawAnimation.begin().then("animation.mimic.idle", Animation.LoopType.LOOP));
            }
        }

        return PlayState.CONTINUE;
    }

    /**
     * サブコントローラ（噛みつきなど短いアクション）
     */
    private <T extends GeoAnimatable> PlayState mouthPredicate(AnimationState<T> event) {
        AnimationController<T> controller = event.getController();

        if (isBiting()) {
            controller.setAnimation(RawAnimation.begin().then("animation.mimic.bite", Animation.LoopType.PLAY_ONCE));
        }

        return PlayState.CONTINUE;
    }

    // ==============================================================
    // 属性設定
    // ==============================================================
    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(
                200.0D, // HP
                0.25D,  // 移動速度
                4.0D,   // 攻撃力
                0.2D,   // ノックバック耐性
                2.0D,   // 防御力
                1.0D    // 重力倍率
        );
    }
}
