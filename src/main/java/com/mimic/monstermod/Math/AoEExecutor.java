package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.List;

/**
 * AoEExecutor
 *
 * 【役割】
 * ・MathMain から AoE の「対象集合」を取得する
 * ・Entity AoE / Block AoE の差異を吸収
 *
 * 【設計原則】
 * ・MathMain.contains = Entity AoE の唯一の真実
 * ・BlockPos 板 = Block AoE の唯一の真実
 * ・副作用を持たない（攻撃・ダメージ処理をしない）
 */
public final class AoEExecutor {

    private AoEExecutor() {}

    /* =========================
     * Entity AoE
     * ========================= */

    public static Collection<LivingEntity> collectEntityTargets(
            ServerLevel level,
            MathMain math
    ) {
        // MathMain.contains に基づいて Entity を列挙するだけ
        return AttackExecutor.collect(level, math);
    }

    /* =========================
     * Block AoE
     * ========================= */

    public static Collection<LivingEntity> collectBlockTargets(
            ServerLevel level,
            MathMain math
    ) {
        // MathMain → BlockPos 板を生成（量子化）
        List<BlockPos> blocks = SamplerBlock2D.sample(math);

        // Block 板を基準に Entity を列挙
        return BlockAttackExecutor.collect(level, blocks);
    }
}
