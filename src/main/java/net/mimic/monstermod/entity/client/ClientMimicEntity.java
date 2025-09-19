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

/**
 * 描画専用 MimicEntity
 * サーバ同期・AI 無効
 */
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
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {

            MimicAnimationState animState = getAnimationState();
            boolean loop = shouldLoop(animState);

            // 状態が変わったときだけアニメーションをセット
            if (lastRequestedAnimation != animState) {
                lastRequestedAnimation = animState;
                state.getController().setAnimation(
                        loop
                                ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                                : RawAnimation.begin().thenPlay(getAnimationName(animState))
                );

                System.out.println("[ClientMimicEntity] play animation: " + getAnimationName(animState) + " loop=" + loop);
            }

            // 非ループの終了処理
            if (!loop && state.getController().hasAnimationFinished()) {
                switch (animState) {
                    case OPENING -> setAnimationState(MimicAnimationState.OPEN);
                    case CLOSING -> setAnimationState(MimicAnimationState.CLOSE);
                    case BITE -> setAnimationState(MimicAnimationState.OPEN);
                    default -> {}
                }
                lastRequestedAnimation = null;
            }

            return PlayState.CONTINUE;
        }));
    }

    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.renderX = x;
        this.renderY = y;
        this.renderZ = z;
        this.renderYRot = yRot;
        this.renderXRot = xRot;
    }

    @Override
    public void setAnimationState(MimicAnimationState state) {
        super.setAnimationState(state);
        this.animationState = state;
        // ★ 強制的にリセットして次フレームで必ず反映
        this.lastRequestedAnimation = null;
    }

    public double getRenderX() {
        return renderX;
    }

    public double getRenderY() {
        return renderY;
    }

    public double getRenderZ() {
        return renderZ;
    }

    public float getRenderYRot() {
        return renderYRot;
    }

    public float getRenderXRot() {
        return renderXRot;
    }

    public MimicAnimationState getRenderAnimationState() {
        return animationState;
    }

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

    @Override
    public void tick() {
        super.tick();
        // 状態更新のみ、アニメーションの tick は GeoEntity が render 時に自動処理
    }
}
