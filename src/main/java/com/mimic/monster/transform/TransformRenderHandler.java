package com.mimic.monster.transform;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
public class TransformRenderHandler {

    // キャッシュ用
    private static LivingEntity cachedEntity;

    @SubscribeEvent
    public static void onRenderPlayer(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(data -> {
            if (!data.isTransformed() || data.getTransformedType() == null) {
                return;
            }

            EntityType<?> type = data.getTransformedType();

            // cachedEntity が null または型が違う、もしくは除去済みなら再生成
            if (cachedEntity == null || cachedEntity.getType() != type || cachedEntity.isRemoved()) {
                cachedEntity = (LivingEntity) type.create(player.level());
            }

            if (cachedEntity != null) {
                cachedEntity.copyPosition(player);
                cachedEntity.setYRot(player.getYRot());
                cachedEntity.setXRot(player.getXRot());
                cachedEntity.tickCount = player.tickCount;

                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource buffer = event.getMultiBufferSource();
                int packedLight = event.getPackedLight();

                EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                var renderer = dispatcher.getRenderer(cachedEntity);
                if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
                    @SuppressWarnings("unchecked")
                    LivingEntityRenderer<LivingEntity, ?> castedRenderer =
                            (LivingEntityRenderer<LivingEntity, ?>) livingRenderer;

                    castedRenderer.render(cachedEntity, player.getYRot(), event.getPartialTick(), poseStack, buffer, packedLight);
                    event.setCanceled(true);
                }
            }
        });
    }
}