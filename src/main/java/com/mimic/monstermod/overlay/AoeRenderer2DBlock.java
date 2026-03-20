package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.Math.MeshBlockConverter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * AoeRenderer2DBlock（Mesh版）
 * 【役割】
 * ・Mesh → Block に変換して描画
 * 【設計】
 * ・Sampler完全排除
 * ・Meshが唯一の真実
 */
public final class AoeRenderer2DBlock {

    private static final float Y_OFFSET = 0.002f;

    private AoeRenderer2DBlock() {}

    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            MathMain math,
            Level level,
            ResourceLocation texture,
            Vec3 playerPos
    ) {

        List<BlockPos> blocks = MeshBlockConverter.toBlocks(math, level, playerPos);

        if (blocks.isEmpty()) return;

        VertexConsumer vc =
                buffers.getBuffer(RenderType.entityTranslucent(texture));

        PoseStack.Pose pose = poseStack.last();

        for (BlockPos pos : blocks) {
            drawTopQuad(vc, pose, pos);
        }
    }

    private static void drawTopQuad(
            VertexConsumer vc,
            PoseStack.Pose pose,
            BlockPos pos
    ) {
        float x0 = pos.getX();
        float x1 = x0 + 1f;
        float z0 = pos.getZ();
        float z1 = z0 + 1f;
        float y  = pos.getY() + Y_OFFSET;

        put(vc, pose, x0, y, z0, 0, 0);
        put(vc, pose, x1, y, z0, 1, 0);
        put(vc, pose, x1, y, z1, 1, 1);
        put(vc, pose, x0, y, z1, 0, 1);
    }

    private static void put(
            VertexConsumer vc,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v
    ) {
        vc.vertex(pose.pose(), x, y, z)
                .color(1f, 0f, 0f, 0.35f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY) // ← ここ重要
                .uv2(240)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();
    }
}