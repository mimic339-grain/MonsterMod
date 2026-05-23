package com.mimic.monstermod.util;

import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

public class HitboxRenderUtil {
    public static void renderIfEnabled(Entity entity, PoseStack poseStack, MultiBufferSource buffer) {
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        localPlayer.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.hasState(IPlayerData.STATE_SHOW_SKILL_LEAD)) {

                boolean isTarget = (entity == localPlayer)
                        || (entity instanceof com.mimic.monstermod.entity.BaseEntity)
                        || (entity instanceof com.mimic.monstermod.entity.obj.OnibiEntity);

                if (!isTarget) return;

                float r = cap.getLeadR();
                float g = cap.getLeadG();
                float b = cap.getLeadB();
                float thickness = cap.getLeadThickness(); // ★ 太さを取得

                net.minecraft.world.phys.AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
                com.mojang.blaze3d.vertex.VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

                // ★ ループを回して「太さ」を表現
                // 0.002ずつ判定箱を膨らませて重ね描きする
                int passes = Math.max(1, (int)thickness);
                for (int i = 0; i < passes; i++) {
                    float offset = i * 0.002f;
                    LevelRenderer.renderLineBox(poseStack, consumer, box.inflate(offset), r, g, b, 1.0F);
                }
            }
        });
    }
}