package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private PlayerRendererMixin() {
        super(null, null, 0f);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            MimicIdentity identity = (MimicIdentity) transformation.getTransformedIdentity();
            if (identity == null) return;

            UUID playerId = player.getUUID();
            ClientMimicEntity mimicEntity = ClientMimicEntity.getOrCreate(playerId);

            // 座標・回転更新（変更があるときだけ）
            mimicEntity.setPosAndRotIfChanged(
                    player.getX(), player.getY(), player.getZ(),
                    player.yBodyRot, player.getXRot()
            );

            // アニメーション更新（必要な時のみ）
            mimicEntity.updateAnimation();

            // 描画
            poseStack.pushPose();

            // 座標補正を最小限にする（必要ならここで調整）
            poseStack.translate(0, 0, 0);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-player.yBodyRot));

            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(mimicEntity)
                    .render(mimicEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

            poseStack.popPose();

            // 元プレイヤー描画をスキップ
            ci.cancel();
        });
    }
}
