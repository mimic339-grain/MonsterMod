package com.mimic.monstermod.Math;

import com.mimic.monstermod.skill.SkillLead;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AttackExecutor {

    private AttackExecutor() {}

    /**
     * @param math スキル発動時に確定した MathMain (ここが origin を持っている)
     */
    public static Collection<LivingEntity> collect(
            ServerLevel level,
            SkillLead skill,
            MathMain math, // 引数に追加
            LivingEntity caster
    ) {
        // ★ followCaster が true の場合のみ、最新のキャスター座標で上書きする
        if (skill.followCaster) {
            math.origin = caster.position();
        }

        // 判定用の広域 AABB 計算
        AABB broad = computeBroadAABB(math);

        List<LivingEntity> candidates = level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                broad,
                LivingEntity::isAlive
        );

        Collection<LivingEntity> result = new ArrayList<>();

        for (LivingEntity target : candidates) {
            if (target.getUUID().equals(caster.getUUID())) continue;

            // 保存された math (origin 座標入り) で判定
            if (isHit(target, math)) {
                result.add(target);
            }
        }

        return result;
    }

    private static boolean isHit(LivingEntity target, MathMain math) {
        Vec3 pos = target.position();
        float h = target.getBbHeight();
        return math.contains(pos) ||
                math.contains(pos.add(0, h * 0.5, 0)) ||
                math.contains(pos.add(0, h, 0));
    }

    private static AABB computeBroadAABB(MathMain math) {
        Vec3 o = math.origin;
        // 形状ごとの最大サイズから AABB を作成
        float r = Math.max(math.radius, Math.max(math.xRadius, math.zRadius));
        float h = Math.max(math.height, math.depth);

        // 余裕を持たせた判定広場
        float s = Math.max(r, h) + 2.0f;
        return new AABB(o.x - s, o.y - s, o.z - s, o.x + s, o.y + s, o.z + s);
    }
}