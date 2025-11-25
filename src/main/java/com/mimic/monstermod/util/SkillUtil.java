package com.mimic.monstermod.util;

import com.mimic.monstermod.Math.AttackArea;
import com.mimic.monstermod.client.preview.AoeMarkerUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SkillUtil {

    public static class SkillConfig {
        // 攻撃対象制御
        public boolean affectSelf = false;       // 自分に当たるか
        public boolean targetSameTeam = false;   // true: 同チームのみ / false: 敵チームのみ

        // 効果内容（複数選択可）
        public boolean isDamage = false;
        public boolean isHealing = false;
        public boolean isBuff = false;
        public boolean isDebuff = false;

        // 形状・向き・範囲
        public Direction direction = Direction.NORTH;
        public double radius = 1.0;
        public double yRadius = 1.0;
        public double xRadius = 1.0;
        public double zRadius = 1.0;
        public double base = 1.0;

        // 攻撃予測用上下制限
        public double maxYDiff = 3.0;
        public double minYDiff = -3.0;
    }

    /** 攻撃範囲座標生成（3D判定用） */
    public static List<double[]> generateArea(SkillConfig config, Vec3 center, String shape) {
        return switch (shape) {
            case "CIRCLE2D" -> AttackArea.getCircle2D(center.x, center.y, center.z, config.radius, false);
            case "CIRCLE3D" -> AttackArea.getCircle3D(center.x, center.y, center.z, config.radius, config.yRadius, false);
            case "RECT2D" -> AttackArea.getRect2D(center.x, center.y, center.z, config.xRadius, config.zRadius, false);
            case "RECT3D" -> AttackArea.getRect3D(center.x, center.y, center.z, config.xRadius, config.yRadius, config.zRadius, false);
            case "FAN2D" -> AttackArea.getFan2D(center.x, center.y, center.z, config.direction, config.radius, 90, false);
            case "FAN3D" -> AttackArea.getFan3D(center.x, center.y, center.z, config.direction, config.radius, 90, config.yRadius, false);
            case "TRIANGLE2D" -> AttackArea.getTriangle2D(center.x, center.y, center.z, config.base, config.direction, false);
            case "TRIANGLE3D" -> AttackArea.getTriangle3D(center.x, center.y, center.z, config.base, config.direction, config.yRadius, false);
            default -> throw new IllegalArgumentException("Unknown shape: " + shape);
        };
    }

    /** 3D攻撃予測マーカー登録 */
    public static void registerAoeMarker(SkillConfig config, Vec3 center, String shape, long durationMs) {
        switch (shape) {
            case "CIRCLE2D" -> AoeMarkerUtil.addCircle2D(center, config.radius, durationMs);
            case "CIRCLE3D" -> AoeMarkerUtil.addCircle3D(center, config.radius, config.yRadius, durationMs);
            case "RECT2D" -> AoeMarkerUtil.addRect2D(center, config.xRadius, config.zRadius, durationMs);
            case "RECT3D" -> AoeMarkerUtil.addRect3D(center, config.xRadius, config.yRadius, config.zRadius, durationMs);
            case "FAN2D" -> AoeMarkerUtil.addFan2D(center, config.direction, config.radius, 90, durationMs);
            case "FAN3D" -> AoeMarkerUtil.addFan3D(center, config.direction, config.radius, 90, config.yRadius, durationMs);
            case "TRIANGLE2D" -> AoeMarkerUtil.addTriangle2D(center, config.direction, config.base, durationMs);
            case "TRIANGLE3D" -> AoeMarkerUtil.addTriangle3D(center, config.direction, config.base, config.yRadius, durationMs);
        }
    }

    /** 2D攻撃予測マーカー登録（段差・上下制限対応） */
    public static void add2DAoePreview(SkillConfig config, Vec3 center, String shape, long durationMs, Level level) {
        List<double[]> area2D = generateArea(config, center, shape); // XZ座標リスト
        for (double[] pos : area2D) {
            int x = (int) pos[0];
            int z = (int) pos[2];
            int groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (groundY >= center.y + config.minYDiff && groundY <= center.y + config.maxYDiff) {
                Vec3 markerPos = new Vec3(x + 0.5, groundY + 0.1, z + 0.5);
                registerAoeMarker(config, markerPos, shape, durationMs);
            }
        }
    }

    /** 攻撃判定（座標リスト + フラグ管理） */
    public static void applySkillEffect(LivingEntity attacker, List<LivingEntity> targets, List<double[]> area, SkillConfig config) {
        for (LivingEntity target : targets) {
            if (!canAffect(attacker, target, config)) continue;

            // 攻撃範囲内にいるか判定
            Vec3 pos = target.position();
            boolean inArea = area.stream().anyMatch(p ->
                    Math.abs(p[0] - pos.x) < 0.5 &&
                            Math.abs(p[1] - pos.y) < 0.5 &&
                            Math.abs(p[2] - pos.z) < 0.5
            );
            if (!inArea) continue;

            // スキル効果適用
            if (config.isDamage) applyDamage(attacker, target);
            if (config.isHealing) applyHealing(attacker, target);
            if (config.isBuff) applyBuff(attacker, target);
            if (config.isDebuff) applyDebuff(attacker, target);
        }
    }

    /** 同チーム判定＋対象判定 */
    private static boolean canAffect(LivingEntity attacker, LivingEntity target, SkillConfig config) {
        if (!config.affectSelf && target == attacker) return false;

        boolean sameTeam = (attacker.getTeam() != null && attacker.getTeam() == target.getTeam());

        // 同チーム向け / 敵チーム向けの判定
        return config.targetSameTeam == sameTeam;
    }

    private static void applyDamage(LivingEntity attacker, LivingEntity target) {
        target.hurt(attacker.damageSources().mobAttack(attacker), 5f);
    }

    private static void applyHealing(LivingEntity attacker, LivingEntity target) {
        target.heal(5f);
    }

    private static void applyBuff(LivingEntity attacker, LivingEntity target) {
        // TODO: バフ処理（例: 効果時間, 効果内容）
    }

    private static void applyDebuff(LivingEntity attacker, LivingEntity target) {
        // TODO: デバフ処理（例: 毒, スローなど）
    }
}
