package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
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

    // --- 位置・回転同期用 ---
    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;

    public ClientMimicEntity() {
        super(ModEntities.MIMIC.get(), Minecraft.getInstance().level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState animState = getAnimationState();
            if (animState == null) return PlayState.CONTINUE;

            boolean loop = shouldLoop(animState);

            if (lastRequestedAnimation != animState) {
                state.getController().setAnimation(
                        loop ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                                : RawAnimation.begin().thenPlay(getAnimationName(animState))
                );
                lastRequestedAnimation = animState;
            }

            // 非ループ終了後に baseState に戻す
            if (!loop && state.getController().hasAnimationFinished()) {
                MimicAnimationState nextState = isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE;
                switch (animState) {
                    case OPEN -> nextState = MimicAnimationState.OPEN_IDLE;
                    case CLOSE, BITE -> nextState = isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE;
                }
                setAnimationState(nextState);
                lastRequestedAnimation = null;
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void tick() {
        // --- 非ループアニメ再生中は tick() で baseState を上書きしない ---
        if (lastRequestedAnimation != null && !isLoopAnimation(lastRequestedAnimation)) {
            return; // playOnce中は何もしない
        }

        // --- 微振動防止 ---
        boolean moving = this.getDeltaMovement().lengthSqr() > 1e-4;

        MimicAnimationState newState = moving
                ? (isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP)
                : (isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);

        // --- 状態が変わる場合のみ更新 ---
        if (getAnimationState() != newState) {
            setAnimationState(newState);
        }
    }

    // --- 非ループアニメ再生 ---
    public void playOnce(MimicAnimationState state) {
        setAnimationState(state);
        lastRequestedAnimation = state; // tick() による上書きを防ぐ
    }

    public boolean isLoopAnimation(MimicAnimationState state) {
        return shouldLoop(state);
    }

    public boolean isAnimationFinished() {
        return lastRequestedAnimation == null;
    }

    // --- 位置・回転同期 ---
    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        setPos(x, y, z);
        this.renderX = x; this.renderY = y; this.renderZ = z;
        this.renderYRot = yRot; this.renderXRot = xRot;
    }
    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }

    // --- キャッシュ管理 ---
    public static ClientMimicEntity getOrCreate(UUID playerUUID) {
        return CLIENT_ENTITIES.computeIfAbsent(playerUUID, uuid -> new ClientMimicEntity());
    }
    public static void remove(UUID playerUUID) { CLIENT_ENTITIES.remove(playerUUID); }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }
    public static Iterable<UUID> getAllUUIDs() { return CLIENT_ENTITIES.keySet(); }

    public MimicAnimationState getLastRequestedAnimation() {
        return lastRequestedAnimation;
    }
}
