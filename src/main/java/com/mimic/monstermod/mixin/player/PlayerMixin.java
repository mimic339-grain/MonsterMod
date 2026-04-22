package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.mixin.accessor.EntityAccessor;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    // =================================
    // 当たり判定変更
    // =================================
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;
        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // [修正] ここで onLoad を呼ばない！
            // すでに実体がある場合のみサイズを適用する
            if (trans.isTransformed() && trans.getEntity() != null) {
                cir.setReturnValue(trans.getEntity().getDimensions(pose));
            }
        });
    }


    // =================================
    // 目線高さ変更（クライアント側）
    // =================================

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void onGetStandingEyeHeight(Pose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // [修正] ここでも onLoad を呼ばない！
            if (trans.isTransformed() && trans.getEntity() != null) {
                cir.setReturnValue(trans.getEntity().getEyeHeight(pose));
            }
        });
    }
    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravel(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(transform -> {
            BaseEntity transformed = transform.getEntity();
            if (transform.isTransformed() && transformed != null) {
                ((EntityAccessor) player).setMaxUpStep(transformed.getStepHeightValue());
            } else {
                ((EntityAccessor) player).setMaxUpStep(0.6f);
            }
        });
    }
}

