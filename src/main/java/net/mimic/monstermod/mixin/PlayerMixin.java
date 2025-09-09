package net.mimic.monstermod.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Player固有の動作に関するMixin。
 * 例: スニーク時の姿勢変更、特定のインタラクションの制限など。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    // getStepHeight の Mixin は LivingEntityMixin に移動したため、ここからは削除しました。

    // TODO: 必要に応じて、Player固有のメソッドにMixinをインジェクトして挙動を変更
    // 例: isCrouching() や getFluidJumpFactor() など
    // 現状はMimicIdentityで飛行能力を付与しているため、ここで追加の変更は不要かもしれません。

    // 例: プレイヤーがスニークしても姿勢が変わらないようにする (Mimicがしゃがまない場合など)
    // @Inject(method = "isCrouching", at = @At("HEAD"), cancellable = true)
    // private void monstermod_isCrouching(CallbackInfoReturnable<Boolean> cir) {
    //     Player player = (Player)(Object)this;
    //     player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
    //         if (transformation.isTransformed()) {
    //             IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
    //             // ここでIdentityに基づいてスニークを無効化するなどのロジック
    //             // 例: cir.setReturnValue(false); // 常にfalseを返す
    //         }
    //     });
    // }
}