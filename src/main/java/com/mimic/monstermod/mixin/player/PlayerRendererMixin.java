package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerRendererMixin 完全版（YSMMOD 方式）
 * - 変身中のプレイヤーは生成済み BaseMonsterEntity に描画を委譲
 * - Player 本体の描画はキャンセル
 * - partialTicks 補間は Entity 側で処理
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderTransformedEntity(AbstractClientPlayer player,
                                         float entityYaw,
                                         float partialTicks,
                                         PoseStack poseStack,
                                         MultiBufferSource buffer,
                                         int packedLight,
                                         CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (!transformation.isTransformed()) return;

                    BaseMonsterEntity entity = transformation.getEntity();
                    if (entity == null) return;

                    // 描画呼び出し（位置・回転・partialTicks は Entity が持つ）
                    entity.renderOnClient(poseStack, buffer, packedLight, partialTicks);

                    // 通常の Player 描画をキャンセル
                    ci.cancel();
                });
    }
}
