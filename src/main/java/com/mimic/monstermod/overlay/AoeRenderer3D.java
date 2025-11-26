// ------------------- AoeRenderer3D_Fixed.java -------------------
package com.mimic.monstermod.overlay;

import com.mimic.monstermod.overlay.AoeMarkerManager.AoeMarker;
import com.mimic.monstermod.Math.AttackPreview3DMath;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * ワールド座標系で統一された3D AOE描画レンダラー
 * 古いコードと同じ座標系で正確に描画
 */
public class AoeRenderer3D {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation RED_TEXTURE =
            new ResourceLocation("monstermod", "textures/misc/attackpreview.png");

    private static final float SURFACE_ALPHA = 0.3f;
    private static final float EDGE_ALPHA = 1f;

    public static void renderAoeMarkers(PoseStack poseStack, List<AoeMarker> markers) {
        if (markers.isEmpty()) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        for (AoeMarker marker : markers) {
            AttackPreview3DMath.ShapeData shape = marker.get3DShapeData();
            if (shape == null) continue;

            poseStack.pushPose();
            // ワールド座標系の頂点をカメラ相対に変換
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            Matrix4f mat = poseStack.last().pose();

            // 面描画
            VertexConsumer face = bufferSource.getBuffer(RenderType.entityTranslucent(RED_TEXTURE));
            for (Vec3[] quad : shape.surfaces) {
                if (quad == null || quad.length < 3) continue;
                drawFace(mat, face, quad, SURFACE_ALPHA);
            }

            // エッジ描画
            VertexConsumer edge = bufferSource.getBuffer(RenderType.lines());
            for (Vec3[] e : shape.edges) {
                if (e == null || e.length < 2) continue;
                drawEdge(mat, edge, e[0], e[1], EDGE_ALPHA);
            }

            poseStack.popPose();
        }

        bufferSource.endBatch();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawFace(Matrix4f mat, VertexConsumer consumer, Vec3[] quad, float alpha) {
        Vec3 a = quad[0];
        for (int i = 1; i < quad.length - 1; i++) {
            Vec3 b = quad[i];
            Vec3 c = quad[i + 1];
            putVertex(consumer, mat, a, alpha);
            putVertex(consumer, mat, b, alpha);
            putVertex(consumer, mat, c, alpha);
        }
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f mat, Vec3 pos, float alpha) {
        // ワールド座標そのまま使用
        consumer.vertex(mat, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(1f, 1f, 1f, alpha)
                .uv(0f, 0f)
                .overlayCoords(0)
                .uv2(0xF000F0)
                .normal(0f, 1f, 0f)
                .endVertex();
    }

    private static void drawEdge(Matrix4f mat, VertexConsumer consumer, Vec3 start, Vec3 end, float alpha) {
        consumer.vertex(mat, (float) start.x, (float) start.y, (float) start.z)
                .color(1f, 0f, 0f, alpha)
                .normal(0f, 1f, 0f)
                .endVertex();
        consumer.vertex(mat, (float) end.x, (float) end.y, (float) end.z)
                .color(1f, 0f, 0f, alpha)
                .normal(0f, 1f, 0f)
                .endVertex();
    }
}
