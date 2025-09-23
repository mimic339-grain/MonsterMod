package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
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

    private MimicAnimationState animationState = MimicAnimationState.IDLE;
    private MimicAnimationState pendingState = null;
    private int holdTicks = 0;
    private int animationTick = 0; // サーバ同期用 tick

    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;

    public ClientMimicEntity(UUID playerUUID) {
        super(ModEntities.MIMIC.get(), null);
        this.setId(playerUUID.hashCode());
    }

    public static ClientMimicEntity getOrCreate(UUID playerUUID) {
        return CLIENT_ENTITIES.computeIfAbsent(playerUUID, ClientMimicEntity::new);
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

            RawAnimation animation = loop
                    ? RawAnimation.begin().thenLoop(animName)
                    : RawAnimation.begin().thenPlay(animName);

            state.getController().setAnimation(animation);
            return PlayState.CONTINUE;
        }));
    }

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

    /** 通常入力によるアニメーション更新 */
    public void updateAnimation(AbstractClientPlayer player) {
        boolean moving = isMoving(player);

        MimicAnimationState target = moving
                ? (isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP)
                : (isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);

        if (target != animationState) {
            if (pendingState != target) {
                pendingState = target;
                holdTicks = 1;
            } else {
                holdTicks++;
                if (holdTicks >= 3) {
                    animationState = pendingState;
                    pendingState = null;
                    holdTicks = 0;
                }
            }
        } else {
            pendingState = null;
            holdTicks = 0;
        }
    }

    /** サーバ同期でアニメーションを強制更新 */
    public void updateAnimationFromServer(MimicAnimationState serverState, int serverTick, AbstractClientPlayer player) {
        this.animationState = serverState;
        this.animationTick = serverTick;
    }

    public void setPosAndRotIfChanged(double x, double y, double z, float yRot, float xRot) {
        if (this.renderX != x || this.renderY != y || this.renderZ != z
                || this.renderYRot != yRot || this.renderXRot != xRot) {
            this.renderX = x;
            this.renderY = y;
            this.renderZ = z;
            this.renderYRot = yRot;
            this.renderXRot = xRot;
        }
    }

    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getRenderAnimationState() { return animationState; }
    public int getAnimationTick() { return animationTick; }
}
