package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMimicEntity extends MimicEntity implements GeoEntity {

    private static final Map<UUID, ClientMimicEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private MimicAnimationState lastRequestedAnimation = null;

    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;
    private MimicAnimationState animationState = MimicAnimationState.IDLE;

    public ClientMimicEntity() {
        super(ModEntities.MIMIC.get(), Minecraft.getInstance().level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState animState = getAnimationState();
            boolean loop = switch (animState) {
                case OPENJUMP, CLOSEJUMP, OPEN_IDLE, IDLE -> true;
                default -> false;
            };

            if (lastRequestedAnimation != animState) {
                String animName = getAnimationName(animState);
                RawAnimation animation = loop
                        ? RawAnimation.begin().thenLoop(animName)
                        : RawAnimation.begin().thenPlay(animName);
                state.getController().setAnimation(animation);
                lastRequestedAnimation = animState;
            }

            // ノンループアニメーション終了時はアイドルに戻す
            if (!loop && state.getController().hasAnimationFinished()) {
                MimicAnimationState idleState = switch (animState) {
                    case OPEN -> MimicAnimationState.OPEN_IDLE;
                    case CLOSE, BITE -> MimicAnimationState.IDLE;
                    default -> MimicAnimationState.IDLE;
                };
                if (animationState != idleState) {
                    this.animationState = idleState;
                    super.setAnimationState(idleState);
                    lastRequestedAnimation = null;
                }
            }

            return PlayState.CONTINUE;
        }));
    }

    private boolean isPlayerMoving() {
        if (Minecraft.getInstance().player == null) return false;
        Input input = Minecraft.getInstance().player.input;
        return input.up || input.down || input.left || input.right;
    }

    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.renderX = x;
        this.renderZ = z;
        this.renderYRot = yRot;
        this.renderXRot = xRot;

        // Y座標はジャンプ中以外はそのまま維持（applyAnimationAndRenderで補間）
        if (animationState != MimicAnimationState.CLOSEJUMP && animationState != MimicAnimationState.OPENJUMP) {
            this.renderY = y;
        }
    }

    @Override
    public void setAnimationState(MimicAnimationState state) {
        if (this.animationState != state) {
            super.setAnimationState(state);
            this.animationState = state;
        }
    }

    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getRenderAnimationState() { return animationState; }

    public static ClientMimicEntity getOrCreate(UUID playerUUID) {
        return CLIENT_ENTITIES.computeIfAbsent(playerUUID, uuid -> new ClientMimicEntity());
    }

    public static void remove(UUID playerUUID) {
        CLIENT_ENTITIES.remove(playerUUID);
    }

    public static void clearAll() {
        CLIENT_ENTITIES.clear();
    }
}
