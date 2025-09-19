package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private PlayerRendererMixin(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
                                PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                          PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            UUID playerId = player.getUUID();
            ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

            // 位置・回転・速度を同期
            cachedEntity.setDeltaMovement(player.getDeltaMovement());
            cachedEntity.setPosAndRot(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.yBodyRot,   // Y回転のみ
                    player.getXRot()
            );

            // アニメーション更新
            cachedEntity.tick();

            // 描画処理
            poseStack.pushPose();
            poseStack.translate(
                    cachedEntity.getInterpolatedX(partialTicks) - cachedEntity.getX(),
                    cachedEntity.getInterpolatedY(partialTicks) - cachedEntity.getY(),
                    cachedEntity.getInterpolatedZ(partialTicks) - cachedEntity.getZ()
            );

            // ★Y回転のみ適用 (X回転はアニメーションに任せる)
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                    -cachedEntity.getInterpolatedYRot(partialTicks)
            ));

            // MimicEntity を描画
            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(cachedEntity)
                    .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

            poseStack.popPose();

            // プレイヤーモデル描画をキャンセル
            ci.cancel();
        });
    }
}
