package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.entity.BaseEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class MimicEntity extends BaseEntity {

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private AnimationController<MimicEntity> mainController;

    // 現在再生中のスキルアニメ名を保持
    private String currentSkillAnim = null;

    public MimicEntity(EntityType<? extends BaseEntity> type, Level level) {
        super(type, level);
        this.refreshDimensions();
    }
    @Override
    protected EntityDimensions getCustomDimensions() {
        return EntityDimensions.fixed(0.5f, 0.5f);
    }

    @Override
    protected float getCustomEyeHeight(EntityDimensions dimensions) {
        return 0.5f; // ここで固定値を返す
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
    }

    public boolean isOpen() { return this.entityData.get(OPEN); }
    public void setOpen(boolean open) { this.entityData.set(OPEN, open); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        mainController = new AnimationController<>(this, "main", 0, this::mainPredicate);
        controllers.add(mainController);
    }

    private PlayState mainPredicate(AnimationState<MimicEntity> event) {
        AnimationController<MimicEntity> controller = event.getController();

        // IMonsterData の代わりに BaseEntity の getCurrentSkill() を使用
        String currentSkill = getCurrentSkill();

        // skill 再生中判定
        if (currentSkill != null && !currentSkill.isEmpty()) {
            String skillAnim = switch (currentSkill) {
                case "switch" -> isOpen() ? "animation.mimic.close" : "animation.mimic.open";
                case "bite" -> "animation.mimic.bite";
                default -> null;
            };

            if (skillAnim != null) {
                if (!skillAnim.equals(currentSkillAnim)) {
                    controller.setAnimation(RawAnimation.begin().then(skillAnim, Animation.LoopType.PLAY_ONCE));
                    currentSkillAnim = skillAnim;
                }
            }

            // skill アニメ終了判定
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                // サーバー側で状態を確定させる
                if (!level().isClientSide) {
                    if ("switch".equals(currentSkill)) {
                        setOpen(!isOpen());
                    }
                    // IMonsterData.setSkill(null) の代わりに親クラスのメソッドを使用
                    setCurrentSkill(null);
                }
                currentSkillAnim = null; // リセット
            }

            return PlayState.CONTINUE;
        }

        // 移動/idle (通常状態)
        String anim = event.isMoving()
                ? (isOpen() ? "animation.mimic.open_walk" : "animation.mimic.close_walk")
                : (isOpen() ? "animation.mimic.open_idle" : "animation.mimic.idle");

        controller.setAnimation(RawAnimation.begin().then(anim, Animation.LoopType.LOOP));
        currentSkillAnim = null; // スキルではないのでリセット
        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseEntity.createDefaultAttributes(
                100.0D, // HP
                0.25D,  // Speed
                100.0D, // Damage
                1.2D,   // Knockback Resistance
                2.0D    // Armor
        );
    }
}