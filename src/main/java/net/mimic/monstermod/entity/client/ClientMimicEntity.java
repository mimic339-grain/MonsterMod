package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
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

            MimicAnimationState current = animationState;

            boolean loop = switch (current) {
                case IDLE, OPEN_IDLE -> true;
                default -> false;
            };

            // 前回のアニメと違う、またはループアニメは毎フレーム更新
            if (lastRequestedAnimation != current || loop) {
                lastRequestedAnimation = current;

                RawAnimation anim = loop
                        ? RawAnimation.begin().thenLoop(getAnimationName(current))
                        : RawAnimation.begin().thenPlay(getAnimationName(current));

                state.getController().setAnimation(anim);

                System.out.println("[ClientMimicEntity] setAnimation: " + getAnimationName(current) +
                        " loop=" + loop + " tick=" + this.tickCount);
            }

            // ループでないアニメが終わったら自動で IDLE 系に戻す
            if (!loop && state.getController().hasAnimationFinished()) {
                switch (current) {
                    case OPEN -> setAnimationState(MimicAnimationState.OPEN_IDLE);
                    case CLOSE, BITE, OPENJUMP, CLOSEJUMP -> setAnimationState(MimicAnimationState.IDLE);
                    default -> {}
                }
                lastRequestedAnimation = null;
            }

            return PlayState.CONTINUE;
        }));
    }

    public void tickUpdate(double x, double y, double z, float yRot, float xRot, MimicAnimationState state) {
        // 状態更新
        if (this.animationState != state) {
            this.animationState = state;
            lastRequestedAnimation = null; // ←重要: controller が更新を反映できるようにリセット
        }

        // 座標補間
        renderX += (x - renderX);
        renderY += (y - renderY);
        renderZ += (z - renderZ);

        renderYRot = yRot;
        renderXRot = xRot;

        System.out.println("[ClientMimicEntity] tickUpdate pos=("
                + renderX + "," + renderY + "," + renderZ + ") state=" + animationState);
    }

    // Getter / Setter
    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getAnimationState() { return animationState; }
    public void setAnimationState(MimicAnimationState state) {
        if (this.animationState != state) {
            this.animationState = state;
            lastRequestedAnimation = null;
        }
    }

    // UUID キャッシュ
    private static final Map<UUID, ClientMimicEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();
    public static ClientMimicEntity getOrCreate(UUID uuid) {
        return CLIENT_ENTITIES.computeIfAbsent(uuid, u -> new ClientMimicEntity());
    }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }
}
