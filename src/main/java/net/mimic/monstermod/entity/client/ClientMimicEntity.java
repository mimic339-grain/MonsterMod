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

    public ClientMimicEntity() {
        super(ModEntities.MIMIC.get(), Minecraft.getInstance().level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState animState = getAnimationState();
            if (animState == null) return PlayState.CONTINUE;

            boolean loop = shouldLoop(animState);

            // --- 前回と同じアニメーションなら再セットしない ---
            if (lastRequestedAnimation != animState) {
                if (loop) {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop(getAnimationName(animState)));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().thenPlay(getAnimationName(animState)));
                }
                lastRequestedAnimation = animState;
            }

            // --- 非ループアニメーション終了後に baseState に戻す ---
            if (!loop && state.getController().hasAnimationFinished()) {
                switch (animState) {
                    case OPEN -> setAnimationState(MimicAnimationState.OPEN_IDLE);
                    case CLOSE -> setAnimationState(MimicAnimationState.IDLE);
                    case BITE -> setAnimationState(isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);
                    default -> {}
                }
                lastRequestedAnimation = null; // アニメ完了後にだけリセット
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void tick() {
        super.tick();

        // 非ループアニメ再生中は上書きしない
        if (lastRequestedAnimation == null || isLoopAnimation(lastRequestedAnimation)) {
            boolean moving = this.getDeltaMovement().lengthSqr() > 1e-6;
            MimicAnimationState jumpState = moving
                    ? (isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP)
                    : (isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);

            if (getAnimationState() != jumpState) {
                setAnimationState(jumpState);
            }
        }
    }



    // 非ループアニメ再生用
    public void playOnce(MimicAnimationState state) {
        setAnimationState(state);
        lastRequestedAnimation = state; // tick() による上書きをブロック
    }


    public boolean isLoopAnimation(MimicAnimationState state) {
        return shouldLoop(state);
    }

    public boolean isAnimationFinished() {
        return lastRequestedAnimation == null;
    }

    public MimicAnimationState getLastRequestedAnimation() {
        return lastRequestedAnimation;
    }

    // --- 位置・回転同期 ---
    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;

    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.setPos(x, y, z);
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
    public MimicAnimationState getRenderAnimationState() { return getAnimationState(); }

    // --- キャッシュ管理 ---
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
