package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
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

/**
 * クライアント用 MimicEntity
 * プレイヤーごとに独立したアニメーション管理とレンダリング座標を保持
 */
public class ClientMimicEntity extends MimicEntity implements GeoEntity {

    // プレイヤーUUIDごとのクライアントMimicEntity保持マップ
    private static final Map<UUID, ClientMimicEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // 最後に要求したアニメーション
    private MimicAnimationState lastRequestedAnimation = null;

    // レンダリング用座標・回転
    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;

    // 現在のアニメーション状態
    private MimicAnimationState animationState = MimicAnimationState.IDLE;

    // コンストラクタ
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

            // ループアニメーション判定
            boolean loop = switch (animState) {
                case OPENJUMP, CLOSEJUMP, OPEN_IDLE, IDLE -> true;
                default -> false;
            };

            // 状態変化時のみアニメーション設定
            if (lastRequestedAnimation != animState) {
                RawAnimation animation = loop
                        ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                        : RawAnimation.begin().thenPlay(getAnimationName(animState));

                state.getController().setAnimation(animation);
                lastRequestedAnimation = animState;

                System.out.println("[ClientMimicEntity] setAnimation: " + animState);
            }

            // 非ループ系アニメ終了時は自動で待機状態に遷移
            if (!loop && state.getController().hasAnimationFinished()) {
                switch (animState) {
                    case OPEN -> setAnimationState(MimicAnimationState.OPEN_IDLE);
                    case CLOSE, BITE -> setAnimationState(MimicAnimationState.IDLE);
                    default -> { }
                }
                lastRequestedAnimation = null;
                System.out.println("[ClientMimicEntity] Non-loop finished, switching idle");
            }

            return PlayState.CONTINUE;
        }));
    }

    /**
     * レンダリング用の位置・回転を設定
     */
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
    }

    // レンダリング情報取得
    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getRenderAnimationState() { return animationState; }

    // プレイヤーUUIDごとにClientMimicEntityを取得 or 作成
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
