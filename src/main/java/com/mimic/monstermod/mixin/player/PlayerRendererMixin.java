package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerRenderer Mixin 完全版（IdentityMod方式）
 *
 * - 変身中のプレイヤーは BaseMonsterIdentity に描画を完全委譲
 * - Player の回転・ArmPose・装備・Hitbox・EyeHeight は Tick 内で同期済み
 * - partialTicks 補間に対応
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderIdentity(AbstractClientPlayer player,
                                float entityYaw,
                                float partialTicks,
                                PoseStack poseStack,
                                MultiBufferSource buffer,
                                int packedLight,
                                CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (!transformation.isTransformed()) return;

                    BaseMonsterIdentity identity = transformation.getIdentity();
                    if (identity == null) return;

                    // 描画呼び出し（位置・回転は Tick 内で同期済み、ここでは partialTicks 補間のみ）
                    identity.render(player, partialTicks, poseStack, buffer, packedLight);

                    // 通常の Player 描画をキャンセル
                    ci.cancel();
                });
    }
}
