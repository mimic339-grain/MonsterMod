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
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;

import java.util.UUID;

public class MimicEntity extends BaseMonsterEntity<MimicEntity.MimicAnimationState> {

    public enum MimicAnimationState {
        IDLE, OPEN_IDLE, OPEN, CLOSE, OPENJUMP, CLOSEJUMP, BITE
    }

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private int animationTick = 0;

    // --- 状態管理 ---
    private boolean pendingSwitch = false;
    private boolean pendingOpen = false;
    private MimicAnimationState pendingAnimation = null; // 非ループ専用
    private MimicAnimationState currentAnimation = null; // 実際に再生中のアニメーション

    private UUID linkedPlayerUUID = null;

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

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
    public void linkToPlayer(UUID playerUUID) { this.linkedPlayerUUID = playerUUID; }
    public boolean isLinkedTo(Player player) {
        return player != null && player.getUUID().equals(this.linkedPlayerUUID);
    }

    public void requestSwitchAnimation(boolean open) {
        this.pendingSwitch = true;
        this.pendingOpen = open;
    }

    /** 非ループアニメーションをリクエスト */
    public void requestAnimation(MimicAnimationState state) {
        this.pendingAnimation = state;
        this.animationTick = 0;
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
            case IDLE, OPEN_IDLE, OPENJUMP, CLOSEJUMP -> true;
            case OPEN, CLOSE, BITE -> false;
        };
    }

    @Override
    public void tick() {
        super.tick();

        // 移動状態で baseState を変える（pendingAnimation 優先なので上書きしない）
        if (pendingAnimation == null) {
            boolean moving = this.getDeltaMovement().lengthSqr() > 1e-6;
            if (moving) {
                setAnimationState(isOpen() ? MimicAnimationState.OPENJUMP : MimicAnimationState.CLOSEJUMP);
            } else {
                setAnimationState(isOpen() ? MimicAnimationState.OPEN_IDLE : MimicAnimationState.IDLE);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState nextAnim;

            // --- 優先度: pendingAnimation > pendingSwitch > baseState ---
            if (pendingAnimation != null) {
                nextAnim = pendingAnimation;
            } else if (pendingSwitch) {
                nextAnim = pendingOpen ? MimicAnimationState.OPEN : MimicAnimationState.CLOSE;
                pendingSwitch = false;
            } else {
                nextAnim = getAnimationState();
            }

            boolean loop = shouldLoop(nextAnim);

            // 変化があったときだけ setAnimation
            if (currentAnimation != nextAnim) {
                currentAnimation = nextAnim;
                state.getController().setAnimation(loop
                        ? RawAnimation.begin().thenLoop(getAnimationName(nextAnim))
                        : RawAnimation.begin().thenPlay(getAnimationName(nextAnim)));
            }

            // --- 非ループ終了判定 ---
            if (!loop && state.getController().hasAnimationFinished()) {
                switch (currentAnimation) {
                    case OPEN -> setAnimationState(MimicAnimationState.OPEN_IDLE);
                    case CLOSE, BITE -> setAnimationState(MimicAnimationState.IDLE);
                    default -> {}
                }
                pendingAnimation = null;
                currentAnimation = null;
            }

            return PlayState.CONTINUE;
        }));
    }


    @Override
    public boolean isAnimationLocked() {
        return currentAnimation == MimicAnimationState.OPEN ||
                currentAnimation == MimicAnimationState.CLOSE ||
                currentAnimation == MimicAnimationState.BITE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
