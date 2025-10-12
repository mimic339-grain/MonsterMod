package com.mimic.monstermod.mixin;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
/*
    // プレイヤーのヒットボックスサイズを変更
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void monstermod_getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ((LivingEntity)(Object)this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
                    if (currentIdentity != null) {
                        Vec3 dimensions = currentIdentity.getBoundingBoxDimensions(pose);
                        cir.setReturnValue(EntityDimensions.scalable((float) dimensions.x(), (float) dimensions.y()));
                    } else {
                        MonsterMod.getLogger().warn("変身中のプレイヤー {} のIdentityが見つかりません。",
                                player.getName().getString());
                    }
                }
            });
        }
    }

    // プレイヤーの視点の高さを変更
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void monstermod_getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if ((LivingEntity)(Object)this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
                    if (currentIdentity != null) {
                        cir.setReturnValue(currentIdentity.getEyeHeight(pose));
                    } else {
                        MonsterMod.getLogger().warn("変身中のプレイヤー {} のIdentityが見つかりません。",
                                player.getName().getString());
                    }
                }
            });
        }
    }

    // プレイヤーのステップ高さを変更

    // 段差高さ：メソッド経由で設定（Shadow不要）
    @Inject(method = "tick", at = @At("TAIL"))
    private void monstermod_setStepHeight(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                if (trans.isTransformed()) {
                    IPlayerIdentity id = trans.getTransformedIdentity();
                    if (id != null) {
                        // Entity の API を使って安全に設定
                        ((Entity) (Object) this).setMaxUpStep(id.getStepHeight());
                    }
                } else {
                    // 元に戻したい場合は適宜デフォルト値をセット（バニラは 0.6F）
                    ((Entity) (Object) this).setMaxUpStep(0.6F);
                }
            });
        }
    }*/
}