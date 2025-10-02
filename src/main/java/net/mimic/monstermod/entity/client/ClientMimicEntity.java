package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
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
    private MimicAnimationState lastRequestedAnimation = null;
    private int animationTick = 0;
    private int stateHoldTicks = 0;

    private boolean serverSynced = false;
    private int serverSyncCooldown = 0;

    private final Map<String, Boolean> customFlags = new ConcurrentHashMap<>();

    private double renderX, renderY, renderZ;
    private float renderYRot, renderXRot;

    public ClientMimicEntity() {
        super(ModEntities.MIMIC.get(), net.minecraft.client.Minecraft.getInstance().level);
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

            state.getController().setAnimation(loop
                    ? RawAnimation.begin().thenLoop(animName)
                    : RawAnimation.begin().thenPlay(animName)
            );

            return PlayState.CONTINUE;
        }));
    }

    public boolean isMoving() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player instanceof net.minecraft.client.player.LocalPlayer) {
            net.minecraft.client.player.LocalPlayer local = mc.player;
            if (local.input != null) {
                return local.input.up || local.input.down || local.input.left || local.input.right;
            }
        }MonsterMod.getLogger().debug("[isMoving] result={}", isMoving());
        return false;
    }

    private boolean isLooping(MimicAnimationState state) {
        return switch (state) {
            case IDLE, OPEN_IDLE, OPENJUMP, CLOSEJUMP -> true;
            default -> false;
        };
    }

    public void updateAnimation() {
        // serverSynced 状態の更新
        if (serverSynced) {
            serverSyncCooldown--;
            if (serverSyncCooldown <= 0) serverSynced = false;
        }

        boolean moving = isMoving();
        MimicAnimationState targetState = moving
                ? (isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP)
                : (isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);

        // 状態が違う AND serverSynced でない場合のみ状態変更
        if (!serverSynced && targetState != animationState) {
            if (targetState != pendingState) {
                pendingState = targetState;
                stateHoldTicks = 1;
            } else {
                stateHoldTicks++;
                if (stateHoldTicks >= 3) {
                    setAnimationState(pendingState);
                }
            }
        } else {
            pendingState = null;
            stateHoldTicks = 0;
        }

        animationTick++;

        MonsterMod.getLogger().trace("[updateAnimation] player={} state={} tick={} pending={} serverSynced={}",
                this.getUUID(), animationState, animationTick, pendingState, serverSynced);
    }


    public void setAnimationState(MimicAnimationState newState) {
        if (animationState != newState) {
            animationState = newState;
            animationTick = 0;
            pendingState = null;
            stateHoldTicks = 0;
            lastRequestedAnimation = null;
            MonsterMod.getLogger().debug("[setAnimationState] Changing animationState from {} to {} tick={}", animationState, newState, animationTick);
        }

    }

    // ★ サーバー同期用メソッド ★
    public void updateFromServer(MimicAnimationState newState, int tick, Map<String, Boolean> flags) {
        if (newState != null && newState != animationState) {
            animationState = newState;
            animationTick = tick;
            pendingState = null;
            stateHoldTicks = 0;
        } else if (Math.abs(tick - animationTick) > 2) {
            animationTick = tick; // 大きな差分のみ同期
        }

        serverSynced = true;
        serverSyncCooldown = 5;

        customFlags.clear();
        if (flags != null) customFlags.putAll(flags);

        MonsterMod.getLogger().trace("[updateFromServer] player={} state={} tick={} flags={}",
                getUUID(), animationState, animationTick, customFlags);
    }


    public int getAnimationTick() { return animationTick; }
    public boolean isServerSynced() {MonsterMod.getLogger().debug("[serverSynced] value={}", serverSynced);
        return serverSynced; }
    public Map<String, Boolean> getCustomFlags() { return customFlags; }
    public boolean getCustomFlag(String key) { return customFlags.getOrDefault(key, false); }

    public static ClientMimicEntity getOrCreate(UUID playerUUID) {
        return CLIENT_ENTITIES.computeIfAbsent(playerUUID, uuid -> new ClientMimicEntity());
    }
    public static void remove(UUID playerUUID) { CLIENT_ENTITIES.remove(playerUUID); }
    public static void clearAll() { CLIENT_ENTITIES.clear(); }

    // ===== Render pos =====
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

    public void interpolatePosition(double targetX, double targetY, double targetZ, float targetYaw, float targetPitch, float alpha) {
        this.renderX += (targetX - this.renderX) * alpha;
        this.renderY += (targetY - this.renderY) * alpha;
        this.renderZ += (targetZ - this.renderZ) * alpha;
        this.renderYRot += (targetYaw - this.renderYRot) * alpha;
        this.renderXRot += (targetPitch - this.renderXRot) * alpha;
    }

    public double getRenderX() { return renderX; }
    public double getRenderY() { return renderY; }
    public double getRenderZ() { return renderZ; }
    public float getRenderYRot() { return renderYRot; }
    public float getRenderXRot() { return renderXRot; }
    public MimicAnimationState getRenderAnimationState() { return animationState; }
}
