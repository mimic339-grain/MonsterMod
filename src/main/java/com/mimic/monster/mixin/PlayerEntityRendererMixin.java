package com.mimic.monster.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<net.minecraft.client.player.AbstractClientPlayer, net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>> {

    public PlayerEntityRendererMixin(EntityRendererProvider.Context pContext, net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> pModel, float pShadowRadius) {
        super(pContext, pModel, pShadowRadius);
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void monstermod_onRenderPlayer(net.minecraft.client.player.AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transformation.isTransformed()) {
                // ★変更: Capabilityから直接Identityインスタンスを取得
                IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
                if (currentIdentity != null) {
                    // PlayerIdentityRendererを使って変身後のモデルを描画
                    PlayerIdentityRenderer.render(currentIdentity, player, entityYaw, partialTicks, poseStack, buffer, packedLight);
                    // 元のプレイヤーレンダリングをキャンセル
                    ci.cancel();
                }
            }
        });
    }
}