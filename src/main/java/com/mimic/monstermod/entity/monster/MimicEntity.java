package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class MimicEntity extends BaseMonsterEntity {

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private AnimationController<MimicEntity> mainController;

    // 現在再生中のスキルアニメ名を保持
    private String currentSkillAnim = null;

    public MimicEntity(EntityType<? extends BaseMonsterEntity> type, Level level) {
        super(type, level);
    }
    @Override
    public float getEyeHeight(Pose pose) {
        return 0.52f; // 低い目線の Mimic
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(4.6f, 4.7f); // Mimic のサイズ
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
        IMonsterData data = getMonsterData();

        // skill 再生中
        if (data != null && data.getSkill() != null && !data.getSkill().isEmpty()) {
            String skillAnim = switch (data.getSkill()) {
                case "switch" -> isOpen() ? "animation.mimic.close" : "animation.mimic.open";
                case "bite" -> "animation.mimic.bite";
                default -> null;
            };

            if (skillAnim != null) {
                // 再生中アニメと異なる場合のみ setAnimation
                if (!skillAnim.equals(currentSkillAnim)) {
                    System.out.println("[MimicEntity] Play skill animation: " + skillAnim);
                    controller.setAnimation(RawAnimation.begin().then(skillAnim, Animation.LoopType.PLAY_ONCE));
                    currentSkillAnim = skillAnim;
                }
            }

            // skill アニメ終了判定
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                System.out.println("[MimicEntity] Skill animation finished: " + data.getSkill());
                if ("switch".equals(data.getSkill())) setOpen(!isOpen());
                data.setSkill(null);
                currentSkillAnim = null; // リセット
            }

            return PlayState.CONTINUE;
        }

        // 移動/idle
        String anim = event.isMoving()
                ? (isOpen() ? "animation.mimic.open_walk" : "animation.mimic.close_walk")
                : (isOpen() ? "animation.mimic.open_idle" : "animation.mimic.idle");

        controller.setAnimation(RawAnimation.begin().then(anim, Animation.LoopType.LOOP));
        currentSkillAnim = null; // スキルではないのでリセット
        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(
                200.0D,
                0.25D,
                4.0D,
                0.2D,
                2.0D
        );
    }
}
