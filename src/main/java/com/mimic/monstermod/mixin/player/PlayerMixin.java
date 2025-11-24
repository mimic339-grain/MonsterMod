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
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;

        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // クライアント側は描画用 Entity / Identity を初期化
            if (self.level().isClientSide) {
                trans.onLoad(self);
            }

            BaseMonsterEntity transformed = trans.getEntity();
            if (trans.isTransformed() && transformed != null) {
                // サーバー側もクライアント側も transformedEntity が存在すれば適用
                cir.setReturnValue(transformed.getDimensions(pose));
            }
        });
    }

    // =================================
    // 炎エフェクト消去（クライアント側・変身中のみ）
    // =================================
    @Inject(method = "tick", at = @At("HEAD"))
    private void hideFireEffect(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // クライアント専用処理
        if (!self.level().isClientSide()) return;

        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed() && self.isOnFire()) {
                ((EntityAccessor) self).callSetSharedFlag(0, false);
            }
        });
    }

    // =================================
    // 目線高さ変更（クライアント側）
    // =================================
    @Inject(method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F", at = @At("HEAD"), cancellable = true)
    private void onGetStandingEyeHeight(Pose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        self.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // クライアント側のみ初期化
            if (self.level().isClientSide) {
                trans.onLoad((Player) self);
            }
            BaseMonsterEntity transformed = trans.getEntity();
            if (trans.isTransformed() && transformed != null) {
                cir.setReturnValue(transformed.getEyeHeight(pose));
            }
        });
    }
}
