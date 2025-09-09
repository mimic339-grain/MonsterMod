package net.mimic.monstermod.Math;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class SkillUtility {

    private final Cooldown cooldown;

    public SkillUtility(long cooldownMillis) {
        this.cooldown = new Cooldown(cooldownMillis);
    }

    // --------------------
    // 基本攻撃関数
    // --------------------

    /** 立体円形攻撃 */
    public void castCircle(Player caster, int radius, int damage) {
        performSkill(caster, damage, () -> AttackArea.get3DCircle(
                (int) caster.getX(), (int) caster.getY(), (int) caster.getZ(), radius
        ));
    }

    /** 立体四角形攻撃 */
    public void castRect(Player caster, int xRadius, int yRadius, int zRadius, int damage) {
        performSkill(caster, damage, () -> AttackArea.get3DRect(
                (int) caster.getX(), (int) caster.getY(), (int) caster.getZ(),
                xRadius, yRadius, zRadius
        ));
    }

    /** 扇形攻撃（角度指定・立体Y対応） */
    public void castFan(Player caster, int maxDistance, double angleDeg, int yRadius, int damage) {
        performSkill(caster, damage, () -> AttackArea.getFanShape(
                (int) caster.getX(), (int) caster.getY(), (int) caster.getZ(),
                caster.getDirection(), maxDistance, angleDeg, yRadius
        ));
    }

    /** ランダム攻撃ポイント（立体Y対応） */
    public void castRandom(Player caster, int xRange, int yRange, int zRange,
                           int count, int sizeX, int sizeY, int sizeZ, int damage) {
        performSkill(caster, damage, () -> AttackArea.getRandomPoints(
                (int) caster.getX(), (int) caster.getY(), (int) caster.getZ(),
                xRange, yRange, zRange, count, sizeX, sizeY, sizeZ
        ));
    }

    // --------------------
    // 共通処理
    // --------------------
    private void performSkill(Player caster, int damage, Supplier<List<int[]>> targetProvider) {
        if (!cooldown.canUse(caster.getUUID().toString())) return;
        cooldown.use(caster.getUUID().toString());

        Level level = caster.level();
        List<int[]> targets = targetProvider.get();

        showPreview((ServerLevel) level, targets, damage, caster);
    }

    // --------------------
    // パーティクル＆ダメージ処理
    // --------------------
    private void showPreview(ServerLevel level, List<int[]> targets, int damage, Player caster) {
        Set<String> targetSet = new HashSet<>();
        for (int[] t : targets) targetSet.add(t[0] + "," + t[1] + "," + t[2]);

        // パーティクル表示
        for (int[] pos : targets) {
            boolean isBoundary = checkBoundary(pos, targetSet);

            double x = pos[0] + 0.5;
            double y = pos[1] + 0.1;
            double z = pos[2] + 0.5;

            Vector3f color = isBoundary
                    ? new Vector3f(1f, 0f, 0f)
                    : new Vector3f(1f, 100f / 255f, 100f / 255f);
            float size = isBoundary ? 1.0f : 0.8f;

            DustParticleOptions particle = new DustParticleOptions(color, size);

            // 偶数ブロックのみ内部表示
            if (!isBoundary && (pos[0] + pos[2]) % 2 != 0) continue;

            level.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }

        // 1秒後にダメージ処理（caster除外）
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

    // --------------------
    // 外周判定
    // --------------------
    private boolean checkBoundary(int[] pos, Set<String> targetSet) {
        int x = pos[0], y = pos[1], z = pos[2];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!targetSet.contains((x + dx) + "," + y + "," + (z + dz))) return true;
            }
        }
        return false;
    }
}
