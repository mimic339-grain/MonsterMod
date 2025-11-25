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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            List<double[]> coords = markerToCoords(marker);

            // ブロック単位に丸める
            Set<BlockPos> blockSet = new HashSet<>();
            for (double[] p : coords) {
                blockSet.add(new BlockPos((int) Math.floor(p[0]), (int) Math.floor(p[1]), (int) Math.floor(p[2])));
            }

            float alpha = 0.3f + marker.getProgress() * 0.2f;

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(RED_TEXTURE));

            // 外周ブロックのみ描画
            for (BlockPos pos : blockSet) {
                if (!isSurface(pos, blockSet)) continue;
                AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                fillAABB(poseStack, consumer, aabb, alpha, overlay, light);
                LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, 1f, 0f, 0f, 1f);
            }

            poseStack.popPose();
        }

        bufferSource.endBatch();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // 外周判定
    private static boolean isSurface(BlockPos pos, Set<BlockPos> set) {
        for (Direction dir : Direction.values()) {
            if (!set.contains(pos.relative(dir))) return true;
        }
        return false;
    }

    private static void fillAABB(PoseStack poseStack, VertexConsumer consumer, AABB aabb, float alpha, int overlay, int light) {
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

    private static List<double[]> markerToCoords(AoeMarker marker) {
        switch (marker.shape) {
            case CIRCLE2D -> {
                return AttackArea.getCircle2D(marker.center.x, marker.center.y, marker.center.z,
                        marker.radius, marker.includeCenter);
            }
            case CIRCLE3D -> {
                return AttackArea.getCircle3D(marker.center.x, marker.center.y, marker.center.z,
                        marker.radius, marker.yRadius, marker.includeCenter);
            }
            case RECT2D -> {
                return AttackArea.getRect2D(marker.center.x, marker.center.y, marker.center.z,
                        marker.xRadius, marker.zRadius, marker.includeCenter);
            }
            case RECT3D -> {
                return AttackArea.getRect3D(marker.center.x, marker.center.y, marker.center.z,
                        marker.xRadius, marker.yRadius, marker.zRadius, marker.includeCenter);
            }
            case FAN2D -> {
                return AttackArea.getFan2D(marker.center.x, marker.center.y, marker.center.z,
                        marker.direction, marker.radius, marker.angleDeg, marker.includeCenter);
            }
            case FAN3D -> {
                return AttackArea.getFan3D(marker.center.x, marker.center.y, marker.center.z,
                        marker.direction, marker.radius, marker.angleDeg, marker.yRadius, marker.includeCenter);
            }
            case CROSS2D -> {
                return AttackArea.getCross2D(marker.center.x, marker.center.y, marker.center.z,
                        marker.radius, marker.includeCenter);
            }
            case CROSS3D -> {
                return AttackArea.getCross3D(marker.center.x, marker.center.y, marker.center.z,
                        marker.xRadius, marker.yRadius, marker.zRadius, marker.includeCenter);
            }
            case TRIANGLE2D -> {
                return AttackArea.getTriangle2D(marker.center.x, marker.center.y, marker.center.z,
                        marker.base, marker.direction, marker.includeCenter);
            }
            case TRIANGLE3D -> {
                return AttackArea.getTriangle3D(marker.center.x, marker.center.y, marker.center.z,
                        marker.base, marker.direction, marker.yRadius, marker.includeCenter);
            }
            case RANDOM -> {
                return AttackArea.getRandomPoints(marker.center.x, marker.center.y, marker.center.z,
                        marker.xRadius, marker.yRadius, marker.zRadius,
                        marker.count, marker.sizeX, marker.sizeY, marker.sizeZ, marker.use3D);
            }
            case SPHERE -> {
                return AttackArea.getSphere(marker.center.x, marker.center.y, marker.center.z,
                        marker.radius, marker.includeCenter);
            }
            default -> throw new IllegalArgumentException("Unknown shape: " + marker.shape);
        }
    }
}
