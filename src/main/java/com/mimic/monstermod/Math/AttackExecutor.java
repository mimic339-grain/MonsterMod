package com.mimic.monstermod.Math;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AttackExecutor
 *
 * 【役割】
 * ・MathMain.contains に基づき
 *   AoE 内に存在する LivingEntity を列挙する
 *
 * 【設計原則】
 * ・副作用を一切持たない
 * ・Preview / Block / Skill を一切知らない
 * ・MathMain.contains が唯一の真実
 */
public final class AttackExecutor {

    private AttackExecutor() {}

    /* =========================
     * Entry
     * ========================= */

    /**
     * AoE 内に含まれる LivingEntity を列挙する
     */
    public static Collection<LivingEntity> collect(
            ServerLevel level,
            MathMain math
    ) {
        AABB broad = computeBroadAABB(math);

        List<LivingEntity> candidates = level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                broad,
                LivingEntity::isAlive
        );

        Collection<LivingEntity> result = new ArrayList<>();

        for (LivingEntity target : candidates) {

            Vec3 hitPoint = target.position()
                    .add(0, target.getBbHeight() * 0.5, 0);

            if (!math.contains(hitPoint)) continue;

            result.add(target);
        }

        return result;
    }

    /* =========================
     * Broad Phase
     * ========================= */

    private static AABB computeBroadAABB(MathMain math) {
        Vec3 o = math.origin;

        float r = Math.max(
                Math.max(math.radius, math.xRadius),
                Math.max(math.zRadius, math.depth)
        );

        float h = Math.max(math.height, r);

        return new AABB(
                o.x - r, o.y - h, o.z - r,
                o.x + r, o.y + h, o.z + r
        );
    }
}
