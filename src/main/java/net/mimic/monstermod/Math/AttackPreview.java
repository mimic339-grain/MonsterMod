package net.mimic.monstermod.Math;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttackPreview {

    /**
     * 任意の座標リストを渡してパーティクル表示＋1秒後ダメージ処理（caster除外）
     * AttackArea で作った円形・四角形・扇形・ランダム全てに対応可能
     */
    public static void showPrediction(ServerLevel level, List<int[]> targets, int damage, LivingEntity caster) {
        if (targets == null || targets.isEmpty()) return;

        Set<String> targetSet = new HashSet<>();
        for (int[] t : targets) {
            targetSet.add(t[0] + "," + t[1] + "," + t[2]);
        }

        // パーティクル表示
        for (int[] pos : targets) {
            boolean isBoundary = checkBoundary(pos, targetSet);

            double x = pos[0] + 0.5;
            double y = pos[1] + 0.1;
            double z = pos[2] + 0.5;

            DustParticleOptions particle = isBoundary
                    ? new DustParticleOptions(new Vector3f(1f, 0f, 0f), 1.0f)
                    : new DustParticleOptions(new Vector3f(1f, 100f / 255f, 100f / 255f), 0.8f);

            if (!isBoundary && (pos[0] + pos[2]) % 2 != 0) continue;

            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }

        // 1秒後ダメージ処理
        level.getServer().execute(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

            for (int[] pos : targets) {
                double x = pos[0] + 0.5;
                double y = pos[1] + 0.5;
                double z = pos[2] + 0.5;

                AABB box = new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5);

                level.getEntitiesOfClass(LivingEntity.class, box)
                        .forEach(entity -> {
                            if (!entity.equals(caster)) {
                                entity.hurt(level.damageSources().magic(), damage);
                            }
                        });
            }
        });
    }

    /**
     * 周囲8方向＋Yも考慮して外周判定
     */
    private static boolean checkBoundary(int[] pos, Set<String> targetSet) {
        int x = pos[0], y = pos[1], z = pos[2];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                String neighbor = (x + dx) + "," + y + "," + (z + dz);
                if (!targetSet.contains(neighbor)) return true;
            }
        }
        return false;
    }
}
