package com.mimic.monstermod.util;

import com.mimic.monstermod.Math.AttackArea;
import com.mimic.monstermod.Math.AttackPreview2DBlockMath;
import com.mimic.monstermod.client.preview.AoeMarkerManager.Shape;
import com.mimic.monstermod.client.preview.AoeMarkerUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * ★★★★★ 完全版 SkillLeadUtil ★★★★★
 * ・2DBlock → AttackPreview2DBlockMath のみ使用
 * ・3D → 段差処理なし
 * ・2D と 3D の経路を完全分離
 */
public class SkillLeadUtil {

    /** 設定クラス */
    public static class SkillConfig {

        // 対象制御
        public boolean affectSelf = false;
        public boolean targetSameTeam = false;

        // 効果
        public boolean isDamage = false;
        public boolean isHealing = false;
        public boolean isBuff = false;
        public boolean isDebuff = false;

        // 形状
        public Shape shape;
        public Direction direction = Direction.NORTH;

        // 範囲
        public double radius = 1.0;
        public double xRadius = 1.0;
        public double yRadius = 1.0;
        public double zRadius = 1.0;
        public double base = 1.0;
        public double angleDeg = 90;

        // Y制限（2DBlockのみ使用）
        public double minYDiff = -3.0;
        public double maxYDiff = 3.0;
    }

    //-------------------------------------------------------------------------
    // 3D / 2D 描画登録
    //-------------------------------------------------------------------------

    /** AOE描画（3D / 2D 共通だが2DBlockでは直接呼ばない） */
    public static void registerAoeMarker(SkillConfig config, Vec3 center, long duration) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || config.shape == null) return;

        double height = config.yRadius * 2;

        switch (config.shape) {

            // ---- 2D (地形追従しないノーマル2D) ----
            case CIRCLE_2D -> AoeMarkerUtil.addCircle2D(center, config.radius, duration);
            case RECT_2D -> AoeMarkerUtil.addRect2D(center, config.xRadius, config.zRadius, duration);
            case CROSS_2D -> AoeMarkerUtil.addCross2D(center, config.radius, duration);
            case FAN_2D -> AoeMarkerUtil.addFan2D(center, config.direction, config.radius, config.angleDeg, duration);
            case TRIANGLE_2D -> AoeMarkerUtil.addTriangle2D(center, config.direction, config.base, duration);
            case RANDOM_2D -> AoeMarkerUtil.addRandom2D(center, config.xRadius, config.zRadius, duration);

            // ---- 3D ----
            case SPHERE -> AoeMarkerUtil.addSphere(level, center, config.radius, duration);
            case BOX -> AoeMarkerUtil.addBox(level, center, config.xRadius, config.yRadius, config.zRadius, duration);
            case CYLINDER -> AoeMarkerUtil.addCylinder(level, center, config.radius, height, duration);
            case CAPSULE -> AoeMarkerUtil.addCapsule(level, center, config.radius, height, duration);
            case FAN3D -> AoeMarkerUtil.addFan3D(level, center, config.direction, config.radius, config.angleDeg, height, duration);
            case CROSS3D -> AoeMarkerUtil.addCross3D(level, center, config.xRadius, config.yRadius, config.zRadius, duration);
            case TRIANGLE_PRISM ->
                    AoeMarkerUtil.addTrianglePrism(level, center, config.direction, config.base, height, config.zRadius, duration);

            // ※ 2DBlock はここでは描画しない（下で専用処理）
            default -> {}
        }
    }

    //-------------------------------------------------------------------------
    // ★ 2DBlock 専用プレビュー（段差処理完全版）
    //-------------------------------------------------------------------------

    /**
     * AttackPreview2DBlockMath 専用の地形追従2DBlockプレビュー
     */
    public static void add2DPreviewBlockOnly(SkillConfig config, Vec3 center, long duration, Level level) {

        AttackPreview2DBlockMath.ShapeType shape = switch (config.shape) {
            case CIRCLE_BLOCK2D -> AttackPreview2DBlockMath.ShapeType.CIRCLEBlock2D;
            case FAN_BLOCK2D -> AttackPreview2DBlockMath.ShapeType.FANBlock2D;
            case RECT_BLOCK2D -> AttackPreview2DBlockMath.ShapeType.RECTBlock2D;
            case TRIANGLE_BLOCK2D -> AttackPreview2DBlockMath.ShapeType.TRIANGLEBlock2D;
            case CROSS_BLOCK2D -> AttackPreview2DBlockMath.ShapeType.CROSSBlock2D;
            case RANDOM_BLOCK2D -> AttackPreview2DBlockMath.ShapeType.RANDOMBlock2D;
            default -> null;
        };

        if (shape == null) {
            return; // 2DBlock 以外は使用しない
        }

        // 2DBlock の実計算
        AttackPreview2DBlockMath.Area2DBlock blockArea =
                AttackPreview2DBlockMath.generateAreaWithOutline(
                        level, shape, center,
                        config.radius, config.xRadius, config.zRadius,
                        config.base, config.direction,
                        config.minYDiff, config.maxYDiff, 16
                );

        // ブロック位置をそのまま2D描画
        for (Vec3 p : blockArea.blocks) {
            AoeMarkerUtil.addRect2D(p, 0.5, 0.5, duration);
        }
    }

    //-------------------------------------------------------------------------
    // 当たり判定
    //-------------------------------------------------------------------------
    /** 攻撃範囲座標生成（精密版） */
    public static List<double[]> generateArea(SkillConfig config, Vec3 center) {
        double height = config.yRadius * 2; // 高さ統一
        switch (config.shape) {
            case SPHERE ->   { return AttackArea.getSphere(center.x, center.y, center.z, config.radius, true); }
            case CYLINDER, CIRCLE_2D, CIRCLE_BLOCK2D -> {
                return AttackArea.getCircle3D(center.x, center.y, center.z, config.radius, height, true);
            }
            case BOX ,RECT_2D,RECT_BLOCK2D ->{
                return AttackArea.getRect3D(center.x, center.y, center.z, config.xRadius, height, config.zRadius, true);
            }
            case CROSS3D ,CROSS_2D ,CROSS_BLOCK2D -> {
                return AttackArea.getCross3D(center.x, center.y, center.z, config.xRadius, height, config.zRadius, true);
            }
            case FAN3D ,FAN_2D ,FAN_BLOCK2D-> {
                return AttackArea.getFan3D(center.x, center.y, center.z, config.direction, config.radius, config.angleDeg, height, true);
            }
            case TRIANGLE_PRISM ,TRIANGLE_2D ,TRIANGLE_BLOCK2D-> {
                return AttackArea.getTriangle3D(center.x, center.y, center.z, config.base, config.direction, height, true);
            }
            case CAPSULE,RANDOM_2D, RANDOM_BLOCK2D -> {
                return AttackArea.getRandomPoints(center.x, center.y, center.z, config.xRadius, height, config.zRadius, 10, 1, 1, 1, false);
            }
            default -> { return new ArrayList<>(); }
        }
    }
    /**
     * ★ プレビュー自動振り分け（2DBlock / 2D / 3D 完全対応）★
     * ability から呼ぶのは基本コレ1つでOK
     */
    public static void add2DAoePreview(SkillConfig config, Vec3 center, long duration, Level level) {
        // ---- 地形追従 2DBlock ----
        switch (config.shape) {
            case CIRCLE_BLOCK2D, FAN_BLOCK2D, RECT_BLOCK2D, TRIANGLE_BLOCK2D, CROSS_BLOCK2D, RANDOM_BLOCK2D -> {add2DPreviewBlockOnly(config, center, duration, level);return;}
        }
        // ---- 通常 2D ----
        switch (config.shape) {
            case CIRCLE_2D -> {AoeMarkerUtil.addCircle2D(center, config.radius, duration);return;}
            case RECT_2D -> {AoeMarkerUtil.addRect2D(center, config.xRadius, config.zRadius, duration);return;}
            case CROSS_2D -> {AoeMarkerUtil.addCross2D(center, config.radius, duration);return;}
            case FAN_2D -> {AoeMarkerUtil.addFan2D(center, config.direction, config.radius, config.angleDeg, duration);return;}
            case TRIANGLE_2D -> {AoeMarkerUtil.addTriangle2D(center, config.direction, config.base, duration);return;}
            case RANDOM_2D -> {AoeMarkerUtil.addRandom2D(center, config.xRadius, config.zRadius, duration);return;}
        }
        // ---- それ以外はすべて 3D ----
        registerAoeMarker(config, center, duration);
    }

    //-------------------------------------------------------------------------
    // スキル効果の適用
    //-------------------------------------------------------------------------

    public static void applySkillEffect(LivingEntity attacker, List<LivingEntity> targets,
                                        List<double[]> area, SkillConfig config) {

        for (LivingEntity target : targets) {

            if (!canAffect(attacker, target, config)) continue;

            Vec3 pos = target.position();

            boolean inArea = area.stream().anyMatch(p ->
                    Math.abs(p[0] - pos.x) < 0.5 &&
                            Math.abs(p[1] - pos.y) < 1.0 &&
                            Math.abs(p[2] - pos.z) < 0.5
            );
            if (!inArea) continue;

            if (config.isDamage) applyDamage(attacker, target);
            if (config.isHealing) applyHealing(attacker, target);
            if (config.isBuff) applyBuff(attacker, target);
            if (config.isDebuff) applyDebuff(attacker, target);
        }
    }

    private static boolean canAffect(LivingEntity attacker, LivingEntity target, SkillConfig config) {
        if (!config.affectSelf && target == attacker) return false;
        boolean sameTeam = attacker.getTeam() != null && attacker.getTeam() == target.getTeam();
        return config.targetSameTeam == sameTeam;
    }

    private static void applyDamage(LivingEntity attacker, LivingEntity target) {
        target.hurt(attacker.damageSources().mobAttack(attacker), 5f);
    }

    private static void applyHealing(LivingEntity attacker, LivingEntity target) {
        target.heal(5f);
    }

    private static void applyBuff(LivingEntity attacker, LivingEntity target) {}
    private static void applyDebuff(LivingEntity attacker, LivingEntity target) {}
}
