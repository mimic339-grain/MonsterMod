package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public class PlayerRendererMixin {

    /**
     * プレイヤー描画時に変身中なら描画をキャンセル
     */
    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hideTransformedPlayer(AbstractClientPlayer player, float yaw, float partialTicks,
                                       PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            if (cap.isTransformed()) {
                ci.cancel(); // 変身中は描画キャンセル
            }
        });
    }
}
