package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BlockAttackExecutor
 *
 * 【役割】
 * ・SamplerBlock2D が生成した Block 板を基準に
 *   LivingEntity を列挙する
 *
 * 【設計原則】
 * ・BlockPos = 唯一の真実
 * ・MathMain.contains を一切使わない
 * ・副作用を持たない
 * ・Block Overlay / Preview と完全一致
 */
public final class BlockAttackExecutor {

    private BlockAttackExecutor() {}

    /**
     * Block AoE に含まれる LivingEntity を列挙する
     */
    public static Collection<LivingEntity> collect(
            ServerLevel level,
            List<BlockPos> blocks
    ) {
        Set<LivingEntity> result = new HashSet<>();

        for (BlockPos pos : blocks) {

            // Block 1 マス分の AABB
            AABB box = new AABB(pos);

            List<LivingEntity> entities = level.getEntities(
                    EntityTypeTest.forClass(LivingEntity.class),
                    box,
                    LivingEntity::isAlive
            );

            result.addAll(entities);
        }

        return result;
    }
}
