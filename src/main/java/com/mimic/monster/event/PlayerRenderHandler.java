package com.mimic.monster.event;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerRenderHandler {

    private static LivingEntity cachedEntity = null;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // プレイヤーを取得
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            if (!cap.isTransformed()) return;

            EntityType<?> type = cap.getTransformedType();
            if (type == null) return;

            if (cachedEntity == null || cachedEntity.getType() != type) {
                var entity = type.create(player.level());
                if (!(entity instanceof LivingEntity living)) return;
                cachedEntity = living;
            }

            // プレイヤーの回転や歩行状態を反映
            cachedEntity.setYRot(player.getYRot());
            cachedEntity.setYBodyRot(player.yBodyRot);
            cachedEntity.setYHeadRot(player.yHeadRot);
            cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
            cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());

            // プレイヤー描画をキャンセル
            event.setCanceled(true);

            // 独自描画
            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(cachedEntity)
                    .render(
                            cachedEntity,
                            player.getYRot(),                  // entityYaw
                            event.getPartialTick(),            // partialTicks
                            event.getPoseStack(),              // PoseStack
                            event.getMultiBufferSource(),      // MultiBufferSource
                            event.getPackedLight()             // packedLight
                    );
        });
    }
}
