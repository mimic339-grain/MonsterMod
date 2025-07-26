package net.mimic.monstermod.common.entity; // この行が変更されました

import net.mimic.monstermod.MonsterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;

public class MimicEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public enum MimicAnimationState {
        IDLE,
        OPENING, // 開き中
        OPEN,    // 開いている
        CLOSING, // 閉じ中
        CLOSED   // 閉じている
    }

    private MimicAnimationState currentAnimationState = MimicAnimationState.IDLE;
    private boolean isBiting = false;

    public boolean isBiting() {
        return this.isBiting;
    }

    public static final ResourceLocation MODEL_RESOURCE = new ResourceLocation(MonsterMod.MOD_ID, "geo/mimic.geo.json");
    public static final ResourceLocation TEXTURE_RESOURCE = new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    public static final ResourceLocation ANIMATION_RESOURCE = new ResourceLocation(MonsterMod.MOD_ID, "animations/mimic.animations.json");


    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
        this.setYRot(0);
        this.setXRot(0);
        this.yHeadRot = 0;
        this.yBodyRot = 0;
    }

    @Override
    protected void registerGoals() {
        // AIゴールがないため空のまま
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.setYRot(0);
            this.setXRot(0);
            this.yHeadRot = 0;
            this.yBodyRot = 0;
        }
    }

    public void setCurrentAnimationState(MimicAnimationState state) {
        if (this.currentAnimationState != state) {
            this.currentAnimationState = state;
            if (this.level().isClientSide() && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: MimicEntity: アニメーション状態変更 -> " + state));
            }
        }
    }

    public MimicAnimationState getCurrentAnimationState() {
        return this.currentAnimationState;
    }

    public void setBiting(boolean biting) {
        if (this.isBiting != biting) {
            this.isBiting = biting;
            if (this.level().isClientSide() && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: MimicEntity: バイト状態変更 -> " + biting));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            AnimationController<?> controller = state.getController();

            String animationToPlay = "none";

            if (isBiting) {
                animationToPlay = "bite";
                controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.PLAY_ONCE));

                if (controller.getAnimationState() == AnimationController.State.STOPPED
                        && controller.getCurrentRawAnimation() != null
                        && animationToPlay.equals(controller.getCurrentRawAnimation().getAnimationName())) {
                    this.setBiting(false);
                    if (this.level().isClientSide() && Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: クライアント: バイトアニメーション終了、isBitingをfalse。"));
                    }
                }
            } else {
                boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1e-6;

                switch (currentAnimationState) {
                    case IDLE:
                        animationToPlay = "idle";
                        controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.LOOP));
                        break;
                    case CLOSED:
                        animationToPlay = "close_idle";
                        controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.LOOP));
                        break;
                    case OPENING:
                        animationToPlay = "open";
                        controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.PLAY_ONCE));

                        if (controller.getAnimationState() == AnimationController.State.STOPPED
                                && controller.getCurrentRawAnimation() != null
                                && animationToPlay.equals(controller.getCurrentRawAnimation().getAnimationName())) {
                            this.setCurrentAnimationState(MimicAnimationState.OPEN);
                            if (this.level().isClientSide() && Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: クライアント: OPENINGアニメーション終了、OPEN状態に移行。"));
                            }
                        }
                        break;
                    case OPEN:
                        if (moving) {
                            animationToPlay = "open_walk";
                            controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.LOOP));
                        } else {
                            animationToPlay = "open_idle";
                            controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.LOOP));
                        }
                        break;
                    case CLOSING:
                        animationToPlay = "close";
                        controller.setAnimation(RawAnimation.begin().then(animationToPlay, Animation.LoopType.PLAY_ONCE));

                        if (controller.getAnimationState() == AnimationController.State.STOPPED
                                && controller.getCurrentRawAnimation() != null
                                && animationToPlay.equals(controller.getCurrentRawAnimation().getAnimationName())) {
                            this.setCurrentAnimationState(MimicAnimationState.CLOSED);
                            if (this.level().isClientSide() && Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: クライアント: CLOSINGアニメーション終了、CLOSED状態に移行。"));
                            }
                        }
                        break;
                }
            }

            if (this.level().isClientSide() && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: MimicEntity Controller: 現在状態=" + currentAnimationState + ", バイト=" + isBiting + ", 再生アニメーション='" + animationToPlay + "'"));
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }
}