package net.mimic.monstermod.mixin.common;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LivingEntityのプロパティをプレイヤーの変身状態に基づいて変更するMixin。
 * ヒットボックス、視点の高さなどを動的に変更します。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // プレイヤーのヒットボックスサイズを変更
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void monstermod_getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        // このMixinが適用される対象がLivingEntityであるため、
        // まずそれがPlayerのインスタンスであるかを確認します。
        if ((LivingEntity)(Object)this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
                    if (currentIdentity != null) {
                        Vec3 dimensions = currentIdentity.getBoundingBoxDimensions(pose);
                        cir.setReturnValue(EntityDimensions.scalable((float) dimensions.x(), (float) dimensions.y()));
                    } else {
                        // 変身中だがIdentityが見つからない場合（エラーケース）
                        MonsterMod.getLogger().warn("変身中のプレイヤー {} のIdentityが見つかりません。", player.getName().getString());
                    }
                }
            });
        }
    }

    // プレイヤーの視点の高さを変更
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void monstermod_getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        // このMixinが適用される対象がLivingEntityであるため、
        // まずそれがPlayerのインスタンスであるかを確認します。
        if ((LivingEntity)(Object)this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.isTransformed() ? transformation.getTransformedIdentity() : null;
                    if (currentIdentity != null) {
                        cir.setReturnValue(currentIdentity.getEyeHeight(pose));
                    } else {
                        // 変身中だがIdentityが見つからない場合（エラーケース）
                        MonsterMod.getLogger().warn("変身中のプレイヤー {} のIdentityが見つかりません。", player.getName().getString());
                    }
                }
            });
        }
    }

    // ★最終修正: プレイヤーのステップ高さを変身状態に基づいて変更
    // Loom (Gradle) のリファレンスマップが機能している場合、この標準名で動作するはずです。
    // これでエラーが出る場合、開発環境のセットアップに根本的な問題がある可能性があります。
    @Inject(method = "getStepHeight()F", at = @At("HEAD"), cancellable = true)
    private void monstermod_getStepHeight_LivingEntity(CallbackInfoReturnable<Float> cir) {
        if ((LivingEntity)(Object)this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
                    if (currentIdentity != null) {
                        cir.setReturnValue(currentIdentity.getStepHeight());
                    } else {
                        MonsterMod.getLogger().warn("変身中のプレイヤー {} のIdentityが見つかりません。", player.getName().getString());
                    }
                }
            });
        }
    }
}