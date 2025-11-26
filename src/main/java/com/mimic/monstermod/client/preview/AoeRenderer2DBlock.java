package com.mimic.monstermod.client.preview;

import com.mimic.monstermod.Math.AttackPreview2DBlockMath;
import com.mimic.monstermod.client.preview.AoeMarkerManager.AoeMarker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Set;

/**
 * AttackPreview2DBlockMath対応 Block2D AoE描画
 * - 面は赤半透明（alpha 0.5）
 * - 枠は赤でライン描画（alpha 1.0）
 */
public class AoeRenderer2DBlock {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation RED_TEXTURE =
            new ResourceLocation("monstermod", "textures/misc/attackpreview.png");
    private static final float SURFACE_ALPHA = 0.5f;
    private static final float EDGE_ALPHA = 1f;

    /** Block2Dマーカーリスト描画 */
    public static void renderAoeMarkers(PoseStack poseStack, List<AoeMarker> markers) {
        if (markers.isEmpty()) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        for (AoeMarker marker : markers) {
            AttackPreview2DBlockMath.Area2DBlock area = marker.getBlock2DArea();
            if (area == null) continue;

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z); // カメラ補正

            // ===== Surface描画（ブロック面） =====
            VertexConsumer surfaceConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(RED_TEXTURE));
            for (Vec3 block : area.blocks) {
                // ブロックを1x1の四角形として描画
                Vec3[] quad = new Vec3[]{
                        new Vec3(block.x - 0.5, block.y, block.z - 0.5),
                        new Vec3(block.x - 0.5, block.y, block.z + 0.5),
                        new Vec3(block.x + 0.5, block.y, block.z + 0.5),
                        new Vec3(block.x + 0.5, block.y, block.z - 0.5)
                };
                drawQuad(poseStack, surfaceConsumer, quad, SURFACE_ALPHA);
            }

            // ===== Edge描画（枠線） =====
            VertexConsumer edgeConsumer = bufferSource.getBuffer(RenderType.lines());
            Set<String> outline = area.outline;
            for (String key : outline) {
                String[] split = key.split(":");
                int x = Integer.parseInt(split[0]);
                int z = Integer.parseInt(split[1]);
                double y = marker.center.y; // Yはマーカーの高さ
                Vec3 p0 = new Vec3(x - 0.5, y, z - 0.5);
                Vec3 p1 = new Vec3(x - 0.5, y, z + 0.5);
                Vec3 p2 = new Vec3(x + 0.5, y, z + 0.5);
                Vec3 p3 = new Vec3(x + 0.5, y, z - 0.5);

                drawLine(poseStack, edgeConsumer, p0, p1, 1f, 0f, 0f, EDGE_ALPHA);
                drawLine(poseStack, edgeConsumer, p1, p2, 1f, 0f, 0f, EDGE_ALPHA);
                drawLine(poseStack, edgeConsumer, p2, p3, 1f, 0f, 0f, EDGE_ALPHA);
                drawLine(poseStack, edgeConsumer, p3, p0, 1f, 0f, 0f, EDGE_ALPHA);
            }

            poseStack.popPose();
        }

        bufferSource.endBatch();
    }

    /** 四角形描画（面用） */
    private static void drawQuad(PoseStack poseStack, VertexConsumer consumer, Vec3[] quad, float alpha) {
        if (quad.length < 3) return;
        Matrix4f mat = poseStack.last().pose();
        for (int i = 0; i < quad.length - 2; i++) {
            vertex(consumer, mat, quad[0], alpha, 0f, 0f);
            vertex(consumer, mat, quad[i + 1], alpha, 1f, 0f);
            vertex(consumer, mat, quad[i + 2], alpha, 1f, 1f);
        }
    }

    /** 頂点描画（面用） */
    private static void vertex(VertexConsumer consumer, Matrix4f mat, Vec3 pos, float alpha, float u, float v) {
        consumer.vertex(mat, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(1f, 0f, 0f, alpha) // 赤色
                .uv(u, v)
                .overlayCoords(0)
                .uv2(0xF000F0)
                .normal(0f, 1f, 0f)
                .endVertex();
    }

    /** 線描画（Edge用） */
    private static void drawLine(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end,
                                 float r, float g, float b, float a) {
        Matrix4f mat = poseStack.last().pose();
        consumer.vertex(mat, (float) start.x, (float) start.y, (float) start.z).color(r, g, b, a).endVertex();
        consumer.vertex(mat, (float) end.x, (float) end.y, (float) end.z).color(r, g, b, a).endVertex();
    }
}
