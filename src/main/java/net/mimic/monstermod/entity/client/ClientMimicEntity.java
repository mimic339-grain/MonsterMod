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

public class ClientMimicEntity extends MimicEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private MimicAnimationState animationState = MimicAnimationState.IDLE;
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
            // ループアニメーション
            boolean loop = true;

            // 前回と違う状態のみ上書き
            if (lastRequestedAnimation != animationState) {
                RawAnimation anim = RawAnimation.begin().thenLoop(getAnimationName(animationState));
                state.getController().setAnimation(anim);
                lastRequestedAnimation = animationState;
                System.out.println("[ClientMimicEntity] Animation changed to: " + animationState);
            }

            return PlayState.CONTINUE;
        }));
    }

    /**
     * 座標・回転を補間する
     * ジャンプ中以外の微小上下差分は補間せず振動を防止
     */
    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        double oldX = renderX, oldY = renderY, oldZ = renderZ;
        float oldYRot = renderYRot, oldXRot = renderXRot;

        // Y座標補間
        double dy = y - renderY;
        if (Math.abs(dy) > 0.01 || animationState == MimicAnimationState.CLOSEJUMP || animationState == MimicAnimationState.OPENJUMP) {
            renderY += dy * 0.5;
        }

        // X,Z 座標は差分補間
        renderX += (x - renderX) * 0.5;
        renderZ += (z - renderZ) * 0.5;

        renderYRot = yRot;
        renderXRot = xRot;

        System.out.println(String.format(
                "[ClientMimicEntity] setPosAndRot old=(%.2f,%.2f,%.2f) new=(%.2f,%.2f,%.2f) rot=(%.2f,%.2f) state=%s",
                oldX, oldY, oldZ, renderX, renderY, renderZ, renderYRot, renderXRot, animationState
        ));
    }

    public void setAnimationState(MimicAnimationState state) {
        if (this.animationState != state) {
            System.out.println("[ClientMimicEntity] setAnimationState from " + this.animationState + " -> " + state);
            this.animationState = state;
            lastRequestedAnimation = null; // 次回Tickでアニメーション再設定可能
        }
    }

    public MimicAnimationState getAnimationState() { return animationState; }
    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }

    // UUID毎キャッシュ
    private static final Map<UUID, ClientMimicEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();
    public static ClientMimicEntity getOrCreate(UUID uuid) {
        return CLIENT_ENTITIES.computeIfAbsent(uuid, u -> new ClientMimicEntity());
    }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }
}
