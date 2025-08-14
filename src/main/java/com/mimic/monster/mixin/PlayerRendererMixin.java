/*

package com.mimic.monster.mixin;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    private LivingEntity cachedEntity = null;

    @Redirect(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void redirectRender(LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer,
                                LivingEntity entity,
                                float yaw,
                                float partialTicks,
                                PoseStack poseStack,
                                MultiBufferSource buffer,
                                int packedLight) {

        if (entity instanceof AbstractClientPlayer player) {
            player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
                if (cap.isTransformed()) {
                    EntityType<?> type = cap.getTransformedType();
                    if (type != null) {
                        if (cachedEntity == null || cachedEntity.getType() != type) {
                            var e = type.create(player.level());
                            if (e instanceof LivingEntity living) cachedEntity = living;
                        }
                        if (cachedEntity != null) {
                            // プレイヤー回転・歩行状態を反映
                            cachedEntity.setYRot(player.getYRot());
                            cachedEntity.setYBodyRot(player.yBodyRot);
                            cachedEntity.setYHeadRot(player.yHeadRot);
                            cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
                            cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());

                            // 独自描画
                            Minecraft.getInstance().getEntityRenderDispatcher()
                                    .getRenderer(cachedEntity)
                                    .render(cachedEntity, yaw, partialTicks, poseStack, buffer, packedLight);
                            return;
                        }
                    }
                }
            });
        }

        // 通常描画
        renderer.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }
}
*/