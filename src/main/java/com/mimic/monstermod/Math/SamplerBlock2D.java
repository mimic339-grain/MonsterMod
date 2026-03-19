package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * SamplerBlock2D
 *
 * 【役割】
 * ・MathMain の AoE を Block 格子へ量子化
 * ・Block 単位の「AoE 板」を生成
 *
 * 【設計原則】
 * ・形状ロジックは MathMain.contains が唯一の真実
 * ・Sampler は「どう量子化するか」だけを知る
 */
public final class SamplerBlock2D {

    private SamplerBlock2D() {}

    /* =========================
     * Public Entry
     * ========================= */

    /**
     * MathMain から Block 板を生成する（標準設定）
     *
     * ・Y = origin.y（足元想定）
     * ・radius = MathMain の最大半径
     * ・判定点 = Block 上面中央
     */
    public static List<BlockPos> sample(MathMain math) {
        int centerY = (int) Math.floor(math.origin.y);

        int radius = (int) Math.ceil(
                Math.max(
                        Math.max(math.radius, math.xRadius),
                        Math.max(math.zRadius, math.depth)
                )
        );

        boolean useTop = true;

        return sample(math, centerY, radius, useTop);
    }

    /* =========================
     * Core
     * ========================= */

    /**
     * Block 板を列挙する
     *
     * @param math    MathMain
     * @param centerY 判定する Y（足元など）
     * @param radius  探索半径（XZ）
     * @param useTop  true = 上面中央 / false = 中心点
     */
    public static List<BlockPos> sample(
            MathMain math,
            int centerY,
            int radius,
            boolean useTop
    ) {
        List<BlockPos> out = new ArrayList<>();

        int minX = (int) Math.floor(math.origin.x - radius);
        int maxX = (int) Math.ceil (math.origin.x + radius);
        int minZ = (int) Math.floor(math.origin.z - radius);
        int maxZ = (int) Math.ceil (math.origin.z + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                boolean inside = useTop
                        ? BlockMain.containsBlockTop   (math, x, centerY, z)
                        : BlockMain.containsBlockCenter(math, x, centerY, z);

                if (inside) {
                    out.add(new BlockPos(x, centerY, z));
                }
            }
        }
        return out;
    }
}
