package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.Math.SamplerBlock2D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * AoeRenderer2DBlock
 *
 * 【役割】
 * ・SamplerBlock2D が生成した「Block 板 AoE」をそのまま描画
 *
 * 【設計原則】
 * ・MathMain.contains を直接使わない
 * ・SamplerBlock2D の結果を唯一の真理とする
 * ・Block 単位（ギザギザ前提）
 * ・XZ 平面専用（上から見る用途）
 *
 * 【用途】
 * ・Block AoE の Preview
 * ・Discrete AoE のロジック検証
 * ・AttackExecutor(Block) と完全同期
 */
public final class AoeRenderer2DBlock {

    /** Z-fighting 回避用の極小オフセット */
    private static final float Y_OFFSET = 0.002f;

    private AoeRenderer2DBlock() {}

    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            MathMain math,
            int centerY,
            int radius,
            boolean useTop,
            ResourceLocation texture
    ) {
        List<BlockPos> blocks =
                SamplerBlock2D.sample(math, centerY, radius, useTop);

        if (blocks == null || blocks.isEmpty()) return;

        VertexConsumer vc =
                buffers.getBuffer(RenderType.entityTranslucent(texture));

        PoseStack.Pose pose = poseStack.last();

        for (BlockPos pos : blocks) {
            drawTopQuad(vc, pose, pos);
        }
    }

    /**
     * Block 上面に 1x1 Quad を描画
     * ・XZ 専用
     * ・下面・側面は描かない
     */
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

        // 上面 Quad（反時計回り）
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
                .color(1f, 0f, 0f, 0.35f)   // 赤・半透明
                .uv(u, v)
                .uv2(0xF000F0)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();
    }
}
