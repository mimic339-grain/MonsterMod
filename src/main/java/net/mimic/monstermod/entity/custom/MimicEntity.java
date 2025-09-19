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
        // 基本状態
        IDLE,       // デフォルト待機（箱が閉じている静止状態）
        CLOSE,      // 箱を閉じて擬態している（定常状態）
        OPEN,       // 箱が開いて戦闘モード（定常状態）
        // 遷移アニメーション
        OPENING,    // CLOSE → OPEN へ移行中
        CLOSING,    // OPEN → CLOSE へ移行中
        // 行動系
        OPENJUMP,   // OPEN中の移動
        CLOSEJUMP,  // CLOSE中の移動
        BITE        // 攻撃
    }

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private int animationTick = 0;
    private boolean idlePlayed = false;
    private boolean pendingSwitch = false;
    private boolean pendingOpen = false;
    private String linkedPlayerUUID = null;

    // MimicEntity のフィールド
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

    // AnimationController に通知
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
            case OPENING -> "animation.mimic.open";
            case CLOSING -> "animation.mimic.close";
            case OPEN -> "animation.mimic.open";
            case CLOSE -> "animation.mimic.close";
            case OPENJUMP -> "animation.mimic.openjump";
            case CLOSEJUMP -> "animation.mimic.closejump";
            case IDLE -> "animation.mimic.idle";
        };
    }

    @Override
    protected boolean shouldLoop(MimicAnimationState state) {
        return switch (state) {
            case OPEN, CLOSE, OPENJUMP, CLOSEJUMP -> true;   // 状態維持はループ
            case OPENING, CLOSING, BITE -> false;           // 遷移・攻撃は一回きり
            case IDLE -> true;                              // idle はループ
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

        // 移動しているか判定
        boolean moving = this.getDeltaMovement().lengthSqr() > 0.01;
        MimicAnimationState current = getAnimationState();

        if (isOpen()) {
            if (moving) {
                if (current != MimicAnimationState.OPENJUMP) {
                    setAnimationState(MimicAnimationState.OPENJUMP);
                }
            } else if (current == MimicAnimationState.OPENJUMP) {
                // 移動終了 → OPENに戻す
                setAnimationState(MimicAnimationState.OPEN);
            }
        } else {
            if (moving) {
                if (current != MimicAnimationState.CLOSEJUMP) {
                    setAnimationState(MimicAnimationState.CLOSEJUMP);
                }
            } else if (current == MimicAnimationState.CLOSEJUMP) {
                // 移動終了 → CLOSEに戻す
                setAnimationState(MimicAnimationState.CLOSE);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {

            MimicAnimationState animState = getAnimationState();

            // pendingSwitch がある場合は強制切り替え
            if (pendingSwitch) {
                setAnimationState(pendingOpen ? MimicAnimationState.OPENING : MimicAnimationState.CLOSING);
                pendingSwitch = false;
                animState = getAnimationState();
            }

            boolean loop = shouldLoop(animState);

            // 状態が変わった時だけ setAnimation
            if (lastRequestedAnimation != animState) {
                lastRequestedAnimation = animState;

                state.getController().setAnimation(
                        loop
                                ? RawAnimation.begin().thenLoop(getAnimationName(animState))
                                : RawAnimation.begin().thenPlay(getAnimationName(animState))
                );

                // デバッグ出力
                System.out.println("[Mimic] play animation: " + getAnimationName(animState) + " loop=" + loop);
            }

            // 非ループ系の終了処理
            if (!loop) {
                animationTick++;
                if (animationTick >= getAnimationDuration(animState)) {
                    switch (animState) {
                        case OPENING -> setAnimationState(MimicAnimationState.OPEN);
                        case CLOSING -> setAnimationState(MimicAnimationState.CLOSE);
                        case BITE -> setAnimationState(MimicAnimationState.OPEN);
                        case IDLE -> setAnimationState(MimicAnimationState.CLOSE);
                        default -> {
                        }
                    }
                    animationTick = 0;
                    lastRequestedAnimation = null; // 次フレームで新しい anim をリクエスト
                }
            } else {
                animationTick = 0;
            }

            return PlayState.CONTINUE;
        }));
    }

    private int getAnimationDuration(MimicAnimationState state) {
        return switch (state) {
            case BITE, OPENING, CLOSING, IDLE -> 10;
            default -> 0;
        };
    }

    @Override
    public boolean isAnimationLocked() {
        MimicAnimationState state = getAnimationState();
        return state == MimicAnimationState.OPENING
                || state == MimicAnimationState.CLOSING
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
