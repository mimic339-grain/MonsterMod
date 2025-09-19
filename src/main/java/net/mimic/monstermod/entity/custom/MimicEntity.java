package net.mimic.monstermod.entity.custom;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.RawAnimation;

public class MimicEntity extends BaseMonsterEntity<MimicEntity.MimicAnimationState> {

    public enum MimicAnimationState {
        IDLE, OPEN_IDLE, CLOSE, OPEN, OPENJUMP, CLOSEJUMP, BITE
    }

    // private のまま static 定義
    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private int animationTick = 0;
    private boolean idlePlayed = false;

    private boolean pendingSwitch = false;
    private boolean pendingOpen = false;
    private String linkedPlayerUUID = null;

    private MimicAnimationState lastRequestedAnimation = null;
    private final AnimatableInstanceCache cache = null; // GeckoLib 用に保持（ClientMimicEntityで再定義）

    public MimicEntity(EntityType<? extends BaseMonsterEntity<?>> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OPEN, false);
    }

    /** 安全に OPEN を取得する */
    public boolean isOpenSafe() {
        Boolean value = this.entityData.get(OPEN);
        return value != null && value;
    }

    /** 安全に OPEN を設定する */
    public void setOpenSafe(boolean open) {
        if (this.entityData != null) {
            this.entityData.set(OPEN, open);
        }
    }

    public void linkToPlayer(String playerUUID) { this.linkedPlayerUUID = playerUUID; }
    public boolean isLinkedTo(Player player) {
        return player != null && player.getUUID().toString().equals(this.linkedPlayerUUID);
    }

    public void requestSwitchAnimation(boolean open) {
        this.pendingSwitch = true;
        this.pendingOpen = open;
    }

    @Override
    protected Class<MimicAnimationState> getAnimationStateClass() { return MimicAnimationState.class; }
    @Override
    protected MimicAnimationState getDefaultAnimationState() { return MimicAnimationState.IDLE; }

    @Override
    protected String getAnimationName(MimicAnimationState state) {
        return switch (state) {
            case BITE -> "animation.mimic.bite";
            case OPEN -> "animation.mimic.open";
            case CLOSE -> "animation.mimic.close";
            case OPENJUMP -> "animation.mimic.openjump";
            case CLOSEJUMP -> "animation.mimic.closejump";
            case IDLE -> "animation.mimic.idle";
            case OPEN_IDLE -> "animation.mimic.open_idle";
        };
    }

    @Override
    protected boolean shouldLoop(MimicAnimationState state) {
        return switch (state) {
            case OPENJUMP, CLOSEJUMP, IDLE, OPEN_IDLE -> true;
            case OPEN, CLOSE, BITE -> false;
        };
    }

    public boolean isIdlePlayed() { return idlePlayed; }
    public void setIdlePlayed(boolean idlePlayed) { this.idlePlayed = idlePlayed; }

    @Override
    public void tick() {
        super.tick();
        boolean moving = this.getDeltaMovement().lengthSqr() > 0.01;
        MimicAnimationState current = getAnimationState();

        if (isOpenSafe()) {
            if (moving && current != MimicAnimationState.OPENJUMP) setAnimationState(MimicAnimationState.OPENJUMP);
            else if (!moving && current == MimicAnimationState.OPENJUMP) setAnimationState(MimicAnimationState.OPEN);
        } else {
            if (moving && current != MimicAnimationState.CLOSEJUMP) setAnimationState(MimicAnimationState.CLOSEJUMP);
            else if (!moving && current == MimicAnimationState.CLOSEJUMP) setAnimationState(MimicAnimationState.CLOSE);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }

    private int getAnimationDuration(MimicAnimationState state) {
        return switch (state) {
            case BITE, OPEN, CLOSE -> 10;
            default -> 0;
        };
    }

    @Override
    public boolean isAnimationLocked() {
        MimicAnimationState state = getAnimationState();
        return state == MimicAnimationState.OPEN || state == MimicAnimationState.CLOSE || state == MimicAnimationState.BITE;
    }
}
