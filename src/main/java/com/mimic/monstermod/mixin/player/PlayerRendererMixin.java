package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            // ★ ClientPlayer が入場したときに onLoad を呼んで identity を生成
            if (player.level().isClientSide) {
                transformation.onLoad(player);
            }

            BaseMonsterIdentity identity = transformation.getIdentity();

            if (identity != null) {
                // クライアント側同期
                identity.copyFromPlayerClient(player);

                // Identity に描画を委譲
                identity.render(player, partialTicks, poseStack, buffer, packedLight);

                // Player 描画はキャンセル
                ci.cancel();
            }
        });
    }

}