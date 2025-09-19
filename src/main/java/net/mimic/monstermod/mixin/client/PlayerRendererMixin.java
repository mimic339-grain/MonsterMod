package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                          PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            UUID playerId = player.getUUID();
            ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

            // 位置・回転・速度の同期
            cachedEntity.setPosAndRot(player.getX(), player.getY(), player.getZ(),
                    player.yBodyRot, player.getXRot());
            cachedEntity.setDeltaMovement(player.getDeltaMovement());

            // tick() 側で非ループアニメも含めて状態管理する
            cachedEntity.tick();

            // 描画のみ
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-player.yBodyRot));
            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(cachedEntity)
                    .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();

            ci.cancel(); // プレイヤー本体は描画しない
        });
    }
}
