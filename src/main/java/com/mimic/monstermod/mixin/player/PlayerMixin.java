package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.mixin.accessor.EntityAccessor;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
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
    // =================================
    // 当たり判定変更（サーバ・クライアント両対応）
    // =================================
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;

        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            BaseMonsterEntity transformed = trans.getEntity();

            if (trans.isTransformed() && transformed != null) {
                cir.setReturnValue(transformed.getDimensions(pose));
            }
            // 変身解除時は何もしない → vanilla の処理に任せる
        });
    }

    // =================================
    // 炎エフェクト消去
    // =================================
    @Inject(method = "tick", at = @At("HEAD"))
    private void hideFireEffect(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (!self.level().isClientSide()) return;

        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed() && self.isOnFire()) { // ← 変身中のみ
                ((EntityAccessor) self).callSetSharedFlag(0, false);
            }
        });
    }

    // =================================
    // 目線高さ変更
    // =================================
    @Inject(
            method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetStandingEyeHeight(Pose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            BaseMonsterEntity transformed = trans.getEntity();
            if (trans.isTransformed() && transformed != null) {
                cir.setReturnValue(transformed.getEyeHeight(pose));
            }
        });
    }

}
