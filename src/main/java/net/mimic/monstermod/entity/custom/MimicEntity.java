
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
        IDLE,
        OPEN_IDLE,
        OPEN,
        CLOSE,
        OPENJUMP,
        CLOSEJUMP,
        BITE
    }

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean idlePlayed = false;
    private boolean pendingSwitch = false;
    private boolean pendingOpen = false;
    private String linkedPlayerUUID = null;

    private MimicAnimationState lastRequestedAnimation = null;

    public MimicEntity(EntityType<? extends BaseMonsterEntity<?>> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public void setOpen(boolean open) {
        this.entityData.set(OPEN, open);
    }

    public void linkToPlayer(String playerUUID) {
        this.linkedPlayerUUID = playerUUID;
    }

    public boolean isLinkedTo(Player player) {
        return player != null && player.getUUID().toString().equals(this.linkedPlayerUUID);
    }

    public void requestSwitchAnimation(boolean open) {
        this.pendingSwitch = true;
        this.pendingOpen = open;
    }

    @Override
    protected Class<MimicAnimationState> getAnimationStateClass() {
        return MimicAnimationState.class;
    }

    @Override
    protected MimicAnimationState getDefaultAnimationState() {
        return MimicAnimationState.IDLE;
    }

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
            case OPENJUMP, CLOSEJUMP, OPEN_IDLE, IDLE -> true;
            case OPEN, CLOSE, BITE -> false;
        };
    }

    public boolean isIdlePlayed() {
        return idlePlayed;
    }

    public void setIdlePlayed(boolean idlePlayed) {
        this.idlePlayed = idlePlayed;
    }

    @Override
    public void tick() {
        super.tick();

        // プレイヤーと Mimic の座標同期ログ
        Player player = this.level().getNearestPlayer(this, 0.1);
        if (player != null && this.isLinkedTo(player)) {
            System.out.println("[MimicEntity] syncPos " +
                    "player=(" + player.getX() + "," + player.getY() + "," + player.getZ() + ") " +
                    "mimic=(" + this.getX() + "," + this.getY() + "," + this.getZ() + ")");
        }
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {

            MimicAnimationState animState = getAnimationState();

            if (pendingSwitch) {
                setAnimationState(pendingOpen ? MimicAnimationState.OPEN : MimicAnimationState.CLOSE);
                pendingSwitch = false;
                animState = getAnimationState();
                System.out.println("[MimicEntity] forced switch -> " + animState);
            }

            boolean loop = shouldLoop(animState);

            if (lastRequestedAnimation != animState) {
                lastRequestedAnimation = animState;

                state.getController().setAnimation(
                        loop
                                ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                                : RawAnimation.begin().thenPlay(getAnimationName(animState))
                );

                System.out.println("[MimicEntity] setAnimation: " + getAnimationName(animState) +
                        " loop=" + loop + " tick=" + this.tickCount);
            }

            // 再生中アニメーションの確認（tick は出せないので省略）
            if (state.getController().getCurrentAnimation() != null) {
                System.out.println("[MimicEntity] playing=" +
                        state.getController().getCurrentAnimation().animation().name() +
                        " entityTick=" + this.tickCount);
            }

            if (!loop && state.getController().hasAnimationFinished()) {
                System.out.println("[MimicEntity] animation finished: " + animState);
                switch (animState) {
                    case OPEN -> setAnimationState(MimicAnimationState.OPEN_IDLE);
                    case CLOSE, BITE -> setAnimationState(MimicAnimationState.IDLE);
                    default -> {}
                }
                lastRequestedAnimation = null;
            }

            return PlayState.CONTINUE;
        }));
    }



    @Override
    public boolean isAnimationLocked() {
        MimicAnimationState state = getAnimationState();
        return state == MimicAnimationState.OPEN
                || state == MimicAnimationState.CLOSE
                || state == MimicAnimationState.BITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
