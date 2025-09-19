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
 * 描画専用 MimicEntity（サーバ同期・AI 無効）
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

        // SynchedEntityData を初期化
        this.defineSynchedData();

        // OPEN の初期値を設定
        this.setOpenSafe(false);
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

            if (lastRequestedAnimation != animState) {
                lastRequestedAnimation = animState;
                state.getController().setAnimation(loop
                        ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                        : RawAnimation.begin().thenPlay(getAnimationName(animState)));
            }

            // 非ループの終了処理
            if (!loop && state.getController().hasAnimationFinished()) {
                switch (animState) {
                    case OPEN -> setAnimationState(MimicAnimationState.OPEN);
                    case CLOSE -> setAnimationState(MimicAnimationState.CLOSE);
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
        this.lastRequestedAnimation = null;
    }

    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getRenderAnimationState() { return animationState; }

    public static ClientMimicEntity getOrCreate(UUID playerUUID) {
        return CLIENT_ENTITIES.computeIfAbsent(playerUUID, uuid -> {
            ClientMimicEntity entity = new ClientMimicEntity();
            entity.setOpenSafe(false); // 初期値保証
            return entity;
        });
    }

    public static void remove(UUID playerUUID) { CLIENT_ENTITIES.remove(playerUUID); }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }

    @Override
    public void tick() {
        super.tick();
    }
}
