package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMimicEntity extends MimicEntity implements GeoEntity {

    private static final Map<UUID, ClientMimicEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private MimicAnimationState animationState = MimicAnimationState.IDLE;
    private MimicAnimationState pendingState = null;
    private int stateHoldTicks = 0;

    private MimicAnimationState lastRequestedAnimation = null;

    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;

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
            String animName = getAnimationName(animationState);
            boolean loop = isLooping(animationState);

            // ループアニメーション中で、前回と同じなら再セットしない
            if (lastRequestedAnimation != animationState || !loop) {
                state.getController().setAnimation(
                        loop ? RawAnimation.begin().thenLoop(animName)
                                : RawAnimation.begin().thenPlay(animName)
                );
                lastRequestedAnimation = animationState;
                System.out.println("[Controller] setAnimation: " + animName + " loop=" + loop);
            }

            return PlayState.CONTINUE;
        }));
    }

    // ループ判定を関数化
    private boolean isLooping(MimicAnimationState state) {
        return switch (state) {
            case IDLE, OPEN_IDLE, OPENJUMP, CLOSEJUMP -> true;
            default -> false;
        };
    }

    public boolean isMoving(AbstractClientPlayer player) {
        if (player instanceof LocalPlayer local && local.input != null) {
            return local.input.up || local.input.down || local.input.left || local.input.right;
        }
        return false;
    }

    public void updateAnimation(AbstractClientPlayer player) {
        boolean moving = isMoving(player);

        MimicAnimationState targetState = moving
                ? (isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP)
                : (isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);

        if (targetState != animationState) {
            if (targetState != pendingState) {
                pendingState = targetState;
                stateHoldTicks = 1;
            } else {
                stateHoldTicks++;
                if (stateHoldTicks >= 3) {
                    setAnimationState(pendingState, player);
                    pendingState = null;
                    stateHoldTicks = 0;
                }
            }
        } else {
            pendingState = null;
            stateHoldTicks = 0;
        }
    }

    private void setAnimationState(MimicAnimationState newState, AbstractClientPlayer player) {
        if (animationState != newState) {
            animationState = newState;
            System.out.println("[ClientMimicEntity] State change -> " + newState +
                    " (player=" + player.getName().getString() + ")");
        }
    }

    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.renderX = x;
        this.renderY = y;
        this.renderZ = z;
        this.renderYRot = yRot;
        this.renderXRot = xRot;
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
    public static void remove(UUID playerUUID) { CLIENT_ENTITIES.remove(playerUUID); }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }
}
