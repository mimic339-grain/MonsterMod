package net.mimic.monstermod.entity.custom;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class MimicEntity extends BaseMonsterEntity<MimicEntity.MimicAnimationState> {

    public enum MimicAnimationState { IDLE, OPENING, OPEN, CLOSING, CLOSED, BITE }

    private int animationTick = 0;
    private boolean pendingSwitch = false;
    private boolean pendingOpen = false;
    private String linkedPlayerUUID = null;

    public MimicEntity(EntityType<? extends BaseMonsterEntity<?>> type, Level level) {
        super(type, level);
    }

    public void linkToPlayer(String uuid) { this.linkedPlayerUUID = uuid; }

    public boolean isLinkedTo(Player player) {
        if (player == null || linkedPlayerUUID == null) return false;
        return linkedPlayerUUID.equals(player.getUUID().toString());
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
        double horizontalSqr = this.getDeltaMovement().horizontalDistanceSqr();
        double vertical = this.getDeltaMovement().y;

        return switch (state) {
            case BITE -> "animation.mimic.bite";
            case OPENING -> "animation.mimic.open";
            case CLOSING -> "animation.mimic.close";
            case OPEN -> (horizontalSqr > 1e-6 && vertical > 0.1) ? "animation.mimic.openjump" :
                    (horizontalSqr > 1e-6 ? "animation.mimic.openjump" : "animation.mimic.open");
            case CLOSED -> (horizontalSqr > 1e-6 && vertical > 0.1) ? "animation.mimic.closejump" :
                    (horizontalSqr > 1e-6 ? "animation.mimic.closejump" : "animation.mimic.close");
            case IDLE -> "animation.mimic.idle";
        };
    }

    @Override
    protected boolean shouldLoop(MimicAnimationState state) { return state == MimicAnimationState.IDLE; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState animState = getAnimationState();

            if (pendingSwitch) {
                setAnimationState(pendingOpen ? MimicAnimationState.OPENING : MimicAnimationState.CLOSING);
                pendingSwitch = false;
            }

            boolean loop = shouldLoop(animState);
            state.getController().setAnimation(loop ?
                    RawAnimation.begin().thenLoop(getAnimationName(animState)) :
                    RawAnimation.begin().thenPlay(getAnimationName(animState)));

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
        return switch (state) { case BITE, OPENING, CLOSING, IDLE -> 10; default -> 0; };
    }

    public boolean isAnimationLocked() {
        MimicAnimationState state = getAnimationState();
        return state == MimicAnimationState.OPENING || state == MimicAnimationState.CLOSING || state == MimicAnimationState.BITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.1D, 4.0D, 0.2D, 2.0D, 0.08D);
    }

    public void playAnimation(String animName, boolean force) {
        try {
            MimicAnimationState state = MimicAnimationState.valueOf(animName.toUpperCase());
            if (force || !isAnimationLocked()) {
                setAnimationState(state);
                animationTick = 0;
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    @Override
    public Packet<ClientGamePacketListener> createSpawnPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
