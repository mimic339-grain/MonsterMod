package com.mimic.monstermod.mixin.client;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
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
 * PlayerRenderer Mixin (YSMMOD方式)
 *
 * - プレイヤーが変身中の場合、BaseMonsterIdentity に描画を完全委譲
 * - partialTicks 補間、座標・回転・ボーンは Identity 側で処理
 * - tickClient は呼ばず、軽量化
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

                    // ★ render 側は補間のみ
                    BaseMonsterEntity entity = identity.getEntity();
                    if (entity != null) {
                        identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);
                    }
                    // PlayerRenderer の通常描画はキャンセル
                    ci.cancel();
                });
    }
}
