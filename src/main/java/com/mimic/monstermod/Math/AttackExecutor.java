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
 * AttackExecutor (完全版)
 * * 【役割】
 * ・MathMain (3D判定) に基づき、対象を列挙する
 * ・キャスター自身は除外する
 */
public final class AttackExecutor {

    private AttackExecutor() {}

    /**
     * AoE 範囲内の LivingEntity を収集する
     * @param level 実行ワールド
     * @param math 3D判定パラメータ (MathMain)
     * @param caster スキル使用者 (除外対象)
     */
    public static Collection<LivingEntity> collect(
            ServerLevel level,
            MathMain math,
            LivingEntity caster
    ) {
        // 1. 大まかな AABB で絞り込み (Broad Phase)
        AABB broad = computeBroadAABB(math);

        List<LivingEntity> candidates = level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                broad,
                LivingEntity::isAlive
        );

        System.out.println("[DEBUG-AOE] Broad Phase Found: " + candidates.size() + " entities.");

        Collection<LivingEntity> result = new ArrayList<>();

        // 2. 詳細な 3D 判定 (Narrow Phase)
        for (LivingEntity target : candidates) {

            // ★ 自分自身は絶対に攻撃対象に含めない
            if (target.getUUID().equals(caster.getUUID())) {
                continue;
            }

            // Entity の「足元」「お腹」「頭」の 3 点で判定
            // これにより、背の高い敵や浮いている敵も逃さない
            if (isHit(target, math)) {
                result.add(target);
                System.out.println("[DEBUG-AOE] Hit Confirm: " + target.getName().getString());
            }
        }

        System.out.println("[DEBUG-AOE] Total Final Targets: " + result.size());
        return result;
    }

    private static boolean isHit(LivingEntity target, MathMain math) {
        Vec3 pos = target.position();
        float h = target.getBbHeight();

        // 3D 空間内での点判定
        return math.contains(pos) ||                       // 足元
                math.contains(pos.add(0, h * 0.5, 0)) ||   // お腹
                math.contains(pos.add(0, h, 0));           // 頭
    }

    private static AABB computeBroadAABB(MathMain math) {
        Vec3 o = math.origin;
        // 形状の最大範囲を考慮した安全な AABB
        float r = Math.max(math.radius, Math.max(math.xRadius, math.zRadius));
        // 回転しても漏れないように 1.5 倍程度の余裕を持たせる
        float safetyR = r * 1.5f;
        float safetyH = Math.max(math.height, safetyR);

        return new AABB(
                o.x - safetyR, o.y - safetyH, o.z - safetyR,
                o.x + safetyR, o.y + safetyH, o.z + safetyR
        );
    }
}