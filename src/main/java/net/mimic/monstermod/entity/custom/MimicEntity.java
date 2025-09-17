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
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;

public class MimicEntity extends BaseMonsterEntity<MimicEntity.MimicAnimationState> {

    public enum MimicAnimationState {
        IDLE, OPENING, OPEN, CLOSING, CLOSED, BITE
    }

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private int animationTick = 0;
    private boolean idlePlayed = false;

    private boolean pendingSwitch = false;
    private boolean pendingOpen = false;

    private String linkedPlayerUUID = null;

    public MimicEntity(EntityType<? extends BaseMonsterEntity<?>> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
    }

    public boolean isOpen() { return this.entityData.get(OPEN); }
    public void setOpen(boolean open) { this.entityData.set(OPEN, open); }
    public void linkToPlayer(String playerUUID) { this.linkedPlayerUUID = playerUUID; }
    public boolean isLinkedTo(Player player) {
        return player != null && player.getUUID().toString().equals(this.linkedPlayerUUID);
    }

    // AnimationController に通知
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
        double horizontalSqr = this.getDeltaMovement().horizontalDistanceSqr();
        double vertical = this.getDeltaMovement().y;

        return switch (state) {
            case BITE -> "animation.mimic.bite";
            case OPENING -> "animation.mimic.open";
            case CLOSING -> "animation.mimic.close";
            case OPEN -> {
                if (horizontalSqr > 1e-6 && vertical > 0.1) yield "animation.mimic.openjump";
                yield horizontalSqr > 1e-6 ? "animation.mimic.openjump" : "animation.mimic.open";
            }
            case CLOSED -> {
                if (horizontalSqr > 1e-6 && vertical > 0.1) yield "animation.mimic.closejump";
                yield horizontalSqr > 1e-6 ? "animation.mimic.closejump" : "animation.mimic.close";
            }
            case IDLE -> "animation.mimic.idle";
        };
    }

    @Override
    protected boolean shouldLoop(MimicAnimationState state) {
        return switch (state) {
            case OPEN, CLOSED -> false;
            case OPENING, CLOSING, BITE -> false;
            case IDLE -> false;
        };
    }

    public boolean isIdlePlayed() { return idlePlayed; }
    public void setIdlePlayed(boolean idlePlayed) { this.idlePlayed = idlePlayed; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState animState = getAnimationState();

            // ★ pendingSwitch がある場合は強制切り替え
            if (pendingSwitch) {
                setAnimationState(pendingOpen ? MimicAnimationState.OPENING : MimicAnimationState.CLOSING);
                pendingSwitch = false;
            }

            boolean loop = shouldLoop(animState);
            state.getController().setAnimation(loop
                    ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                    : RawAnimation.begin().thenPlay(getAnimationName(animState)));

            // 非ループ系の終了処理
            if (!loop) {
                animationTick++;
                if (animationTick >= getAnimationDuration(animState)) {
                    switch (animState) {
                        case OPENING -> setAnimationState(MimicAnimationState.OPEN);
                        case CLOSING -> setAnimationState(MimicAnimationState.CLOSED);
                        case BITE -> setAnimationState(MimicAnimationState.OPEN);
                        case IDLE -> setAnimationState(MimicAnimationState.CLOSED);
                        default -> {}
                    }
                    animationTick = 0;
                }
            } else animationTick = 0;

            return PlayState.CONTINUE;
        }));
    }

    private int getAnimationDuration(MimicAnimationState state) {
        return switch (state) {
            case BITE -> 10;
            case OPENING, CLOSING -> 10;
            case IDLE -> 10;
            default -> 0;
        };
    }

    @Override
    public boolean isAnimationLocked() {
        MimicAnimationState state = getAnimationState();
        return state == MimicAnimationState.OPENING ||
                state == MimicAnimationState.CLOSING ||
                state == MimicAnimationState.BITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}