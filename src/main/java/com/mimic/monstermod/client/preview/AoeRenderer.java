package com.mimic.monstermod.client.preview;

import com.mimic.monstermod.Math.AttackArea;
import com.mimic.monstermod.client.preview.AoeMarkerManager.AoeMarker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import java.util.List;

/** AOEマーカー描画完全版 */
public class AoeRenderer {

    private static final ResourceLocation RED_TEXTURE =
            new ResourceLocation("monstermod", "textures/misc/attackpreview.png");

    public static void renderAoeMarkers(PoseStack poseStack, Camera camera, float partialTicks, List<AoeMarker> markers) {
        if (markers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        var camPos = camera.getPosition();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        int light = 0xF000F0;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (AoeMarker marker : markers) {
            List<int[]> coords = markerToCoords(marker);

            double minX = coords.stream().mapToDouble(c -> c[0]).min().orElse(marker.center.x) - marker.center.x;
            double maxX = coords.stream().mapToDouble(c -> c[0]).max().orElse(marker.center.x) - marker.center.x;
            double minY = coords.stream().mapToDouble(c -> c[1]).min().orElse(marker.center.y) - marker.center.y;
            double maxY = coords.stream().mapToDouble(c -> c[1]).max().orElse(marker.center.y) - marker.center.y;
            double minZ = coords.stream().mapToDouble(c -> c[2]).min().orElse(marker.center.z) - marker.center.z;
            double maxZ = coords.stream().mapToDouble(c -> c[2]).max().orElse(marker.center.z) - marker.center.z;

            AABB aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            float alpha = 0.3f + marker.getProgress() * 0.2f;

            poseStack.pushPose();
            poseStack.translate(marker.center.x - camPos.x, marker.center.y - camPos.y, marker.center.z - camPos.z);

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(RED_TEXTURE));
            fillAABB(poseStack, consumer, aabb, alpha, light, overlay);
            LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, 1f, 0f, 0f, 1f);

            poseStack.popPose();
        }

        bufferSource.endBatch();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void fillAABB(PoseStack poseStack, VertexConsumer consumer, AABB aabb, float alpha, int light, int overlay) {
        Matrix4f mat = poseStack.last().pose();
        // 底面
        vertex(consumer, mat, aabb.minX, aabb.minY, aabb.minZ, alpha, overlay, light, 0f, 0f);
        vertex(consumer, mat, aabb.maxX, aabb.minY, aabb.minZ, alpha, overlay, light, 1f, 0f);
        vertex(consumer, mat, aabb.maxX, aabb.minY, aabb.maxZ, alpha, overlay, light, 1f, 1f);
        vertex(consumer, mat, aabb.minX, aabb.minY, aabb.maxZ, alpha, overlay, light, 0f, 1f);
        // 上面
        vertex(consumer, mat, aabb.minX, aabb.maxY, aabb.minZ, alpha, overlay, light, 0f, 0f);
        vertex(consumer, mat, aabb.maxX, aabb.maxY, aabb.minZ, alpha, overlay, light, 1f, 0f);
        vertex(consumer, mat, aabb.maxX, aabb.maxY, aabb.maxZ, alpha, overlay, light, 1f, 1f);
        vertex(consumer, mat, aabb.minX, aabb.maxY, aabb.maxZ, alpha, overlay, light, 0f, 1f);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f mat,
                               double x, double y, double z,
                               float alpha, int overlay, int light,
                               float u, float v) {
        consumer.vertex(mat, (float)x, (float)y, (float)z)
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 1f, 0f)
                .endVertex();
    }

    private static List<int[]> markerToCoords(AoeMarker marker) {
        switch (marker.shape) {

            case CIRCLE2D -> {
                return AttackArea.getCircle2D(
                        (int) marker.center.x,
                        (int) marker.center.y,   // ← Y座標も渡す
                        (int) marker.center.z,
                        marker.radius,
                        marker.includeCenter
                );
            }

            case CIRCLE3D -> {
                return AttackArea.getCircle3D(
                        (int) marker.center.x,
                        (int) marker.center.y,
                        (int) marker.center.z,
                        marker.radius,
                        marker.yRadius,
                        marker.includeCenter
                );
            }

            case RECT2D -> {
                return AttackArea.getRect2D(
                        (int) marker.center.x,
                        (int) marker.center.y,   // ← Y座標も渡す
                        (int) marker.center.z,
                        marker.xRadius,
                        marker.zRadius,
                        marker.includeCenter
                );
            }

            case RECT3D -> {
                return AttackArea.getRect3D(
                        (int) marker.center.x,
                        (int) marker.center.y,
                        (int) marker.center.z,
                        marker.xRadius,
                        marker.yRadius,
                        marker.zRadius,
                        marker.includeCenter
                );
            }

            case FAN2D -> {
                return AttackArea.getFan2D(
                        (int) marker.center.x,
                        (int) marker.center.y,   // ← Y座標も渡す
                        (int) marker.center.z,
                        marker.direction,
                        marker.radius,
                        marker.angleDeg,
                        marker.includeCenter
                );
            }

            case FAN3D -> {
                return AttackArea.getFan3D(
                        (int) marker.center.x,
                        (int) marker.center.y,
                        (int) marker.center.z,
                        marker.direction,
                        marker.radius,
                        marker.angleDeg,
                        marker.yRadius,
                        marker.includeCenter
                );
            }

            case CROSS2D -> {
                return AttackArea.getCross2D(
                        (int) marker.center.x,
                        (int) marker.center.y,   // ← Y座標も渡す
                        (int) marker.center.z,
                        marker.radius,
                        marker.includeCenter
                );
            }

            case CROSS3D -> {
                return AttackArea.getCross3D(
                        (int) marker.center.x,
                        (int) marker.center.y,
                        (int) marker.center.z,
                        marker.xRadius,
                        marker.yRadius,
                        marker.zRadius,
                        marker.includeCenter
                );
            }

            case TRIANGLE2D -> {
                return AttackArea.getTriangle2D(
                        (int) marker.center.x,
                        (int) marker.center.y,   // ← Y座標も渡す
                        (int) marker.center.z,
                        marker.base,
                        marker.direction,
                        marker.includeCenter
                );
            }

            case TRIANGLE3D -> {
                return AttackArea.getTriangle3D(
                        (int) marker.center.x,
                        (int) marker.center.y,
                        (int) marker.center.z,
                        marker.base,
                        marker.direction,
                        marker.yRadius,
                        marker.includeCenter
                );
            }

            case RANDOM -> {
                return AttackArea.getRandomPoints(
                        (int) marker.center.x,
                        (int) marker.center.y,
                        (int) marker.center.z,
                        marker.xRadius,
                        marker.yRadius,
                        marker.zRadius,
                        marker.count,
                        marker.sizeX,
                        marker.sizeY,
                        marker.sizeZ,
                        marker.use3D
                );
            }

            default -> throw new IllegalArgumentException("Unknown shape: " + marker.shape);
        }
    }
}
