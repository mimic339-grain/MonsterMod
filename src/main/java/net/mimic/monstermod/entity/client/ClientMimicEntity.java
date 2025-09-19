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

    // 再生要求されているアニメーション（外部から playOnce/setAnimationState）
    private MimicAnimationState requestedAnimation = null;
    // 現在のアニメーションがループ終了待ちでロックされているか
    private boolean animationLocked = false;

    // レンダリング用の補間値
    private double prevRenderX, prevRenderY, prevRenderZ;
    private float prevRenderYRot, prevRenderXRot;
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
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState target = requestedAnimation != null ? requestedAnimation : getAnimationState();
            if (target == null) return PlayState.STOP;

            boolean loop = shouldLoop(target);

            if (!animationLocked || requestedAnimation != target) {
                // 新しいアニメーションを設定
                RawAnimation anim = loop
                        ? RawAnimation.begin().thenLoop(getAnimationName(target))
                        : RawAnimation.begin().thenPlay(getAnimationName(target));

                state.getController().setAnimation(anim);

                animationLocked = !loop; // 非ループアニメはロックする
                requestedAnimation = target;
                setAnimationState(target);
            }

            // 非ループアニメが終了したら baseState に戻す
            if (animationLocked && state.getController().hasAnimationFinished()) {
                animationLocked = false;
                requestedAnimation = null;

                MimicAnimationState baseState = isOpen()
                        ? MimicAnimationState.OPEN_IDLE
                        : MimicAnimationState.IDLE;

                setAnimationState(baseState);
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void tick() {
        // 補間用の前フレーム値を保存
        prevRenderX = renderX;
        prevRenderY = renderY;
        prevRenderZ = renderZ;
        prevRenderYRot = renderYRot;
        prevRenderXRot = renderXRot;

        super.tick();

        // 現フレームの値更新
        renderX = getX();
        renderY = getY();
        renderZ = getZ();
        renderYRot = getYRot();
        renderXRot = getXRot();

        // tick 側では「移動アニメーションの自動切替」のみ
        if (!animationLocked && requestedAnimation == null) {
            boolean moving = this.getDeltaMovement().lengthSqr() > 1e-6;
            MimicAnimationState auto = moving
                    ? (isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP)
                    : (isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);

            if (getAnimationState() != auto) {
                setAnimationState(auto);
            }
        }
    }

    // --- 外部から呼ばれる API ---

    /** 非ループアニメーション（攻撃など）を強制再生 */
    public void playOnce(MimicAnimationState state) {
        requestedAnimation = state;
        animationLocked = true;
    }

    /** ループアニメーション（idle など）をセット */
    @Override
    public void setAnimationState(MimicAnimationState state) {
        super.setAnimationState(state);
        if (shouldLoop(state)) {
            requestedAnimation = state;
            animationLocked = false;
        }
    }

    public boolean isAnimationLocked() {
        return animationLocked;
    }

    // --- 補間用 getter ---
    public double getInterpolatedX(float partialTicks) { return prevRenderX + (renderX - prevRenderX) * partialTicks; }
    public double getInterpolatedY(float partialTicks) { return prevRenderY + (renderY - prevRenderY) * partialTicks; }
    public double getInterpolatedZ(float partialTicks) { return prevRenderZ + (renderZ - prevRenderZ) * partialTicks; }
    public float getInterpolatedYRot(float partialTicks) { return prevRenderYRot + (renderYRot - prevRenderYRot) * partialTicks; }
    public float getInterpolatedXRot(float partialTicks) { return prevRenderXRot + (renderXRot - prevRenderXRot) * partialTicks; }

    // --- 位置と回転の強制セット（プレイヤー同期用） ---
    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.setPos(x, y, z);
        this.prevRenderX = this.renderX = x;
        this.prevRenderY = this.renderY = y;
        this.prevRenderZ = this.renderZ = z;
        this.prevRenderYRot = this.renderYRot = yRot;
        this.prevRenderXRot = this.renderXRot = xRot;
    }

    // --- キャッシュ管理 ---
    public static ClientMimicEntity getOrCreate(UUID playerUUID) {
        return CLIENT_ENTITIES.computeIfAbsent(playerUUID, uuid -> new ClientMimicEntity());
    }
    public static void remove(UUID playerUUID) { CLIENT_ENTITIES.remove(playerUUID); }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }
}
