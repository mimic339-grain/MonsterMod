package com.mimic.monstermod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.UUID;

public class HunterEntity extends BaseEntity {
    private UUID playerUUID;
    private boolean isSlim;

    public HunterEntity(EntityType<? extends BaseEntity> type, Level level) {
        super(type, level);
    }

    public void setPlayerInfo(UUID uuid, boolean slim) {
        this.playerUUID = uuid;
        this.isSlim = slim;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean isSlim() { return isSlim; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1. 移動・待機系コントローラー
        controllers.add(new AnimationController<>(this, "movement", 5, event -> {
            // 飛行中
            //if (this.isFlying()) { // 飛行判定のメソッドはBaseEntity等で定義
               // return event.setAndContinue(RawAnimation.begin().thenLoop("player.animation.fly"));
            //}

            // スニーク中
            if (this.isShiftKeyDown()) {
                if (event.isMoving()) {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("player.animation.shift_walk"));
                }
                return event.setAndContinue(RawAnimation.begin().thenLoop("player.animation.shift"));
            }

            // 通常移動
            if (event.isMoving()) {
                if (this.isSprinting()) {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("player.animation.run"));
                }
                return event.setAndContinue(RawAnimation.begin().thenLoop("player.animation.walk"));
            }

            // 待機
            return event.setAndContinue(RawAnimation.begin().thenLoop("player.animation.idle"));
        }));

        // 2. 攻撃・コンボ系コントローラー
        controllers.add(new AnimationController<>(this, "attack", 2, event -> {
            // 基本は何もしない（triggerableAnimで制御するため）
            return PlayState.STOP;
        }).triggerableAnim("sword1",
                // sword1を再生した後、そのままsword1_afterのループに入る設定
                RawAnimation.begin().thenPlay("player.animation.sword1").thenLoop("player.animation.sword1_after")
        ).triggerableAnim("sword2",
                // sword2が呼ばれたら、今のポーズに関係なくsword2を再生
                RawAnimation.begin().thenPlay("player.animation.sword2").thenLoop("player.animation.sword2_after")
        ));
    }
    // HunterEntity.java に追加
    public static AttributeSupplier.Builder createAttributes() {
        return BaseEntity.createDefaultAttributes(
                20.0D,   // 体力 (HP)
                0.3D,    // 移動速度
                2.0D,    // 攻撃力
                0.0D,    // ノックバック耐性
                0.0D     // アーマー
        );
    }
}