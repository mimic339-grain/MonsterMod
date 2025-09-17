package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.custom.MimicEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描画専用の MimicEntity
 * AI・サーバ同期無効
 */
public class ClientMimicEntity extends MimicEntity implements GeoEntity {

    private static final Map<UUID, ClientMimicEntity> CLIENT_ENTITIES = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;
    private MimicAnimationState animationState = MimicAnimationState.IDLE;

    public ClientMimicEntity() {
        super(null, null);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {}

    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.renderX = x;
        this.renderY = y;
        this.renderZ = z;
        this.renderYRot = yRot;
        this.renderXRot = xRot;
    }

    public void setAnimationState(MimicAnimationState state) {
        this.animationState = state;
    }

    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getRenderAnimationState() { return animationState; }

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
