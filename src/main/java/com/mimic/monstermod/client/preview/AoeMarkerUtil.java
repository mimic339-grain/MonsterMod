package com.mimic.monstermod.client.preview;

import com.mimic.monstermod.client.preview.AoeMarkerManager.AoeMarker;
import com.mimic.monstermod.client.preview.AoeMarkerManager.Shape;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * AoEマーカー追加用ユーティリティクラス（完全版）
 * 全形状対応。AoeMarkerManager の double 半径対応版。
 */
public class AoeMarkerUtil {

    // -------------------- CIRCLE --------------------

    public static void addCircle2D(Vec3 center, double radius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.CIRCLE2D,
                radius, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH, true, false, durationMs
        ));
    }

    public static void addCircle3D(Vec3 center, double radius, double yRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.CIRCLE3D,
                radius, radius, yRadius, radius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH, true, false, durationMs
        ));
    }

    // -------------------- RECT --------------------

    public static void addRect2D(Vec3 center, double xRadius, double zRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.RECT2D,
                0, xRadius, 0, zRadius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH, true, false, durationMs
        ));
    }

    public static void addRect3D(Vec3 center, double xRadius, double yRadius, double zRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.RECT3D,
                0, xRadius, yRadius, zRadius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH, true, false, durationMs
        ));
    }

    // -------------------- CROSS --------------------

    public static void addCross2D(Vec3 center, double radius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.CROSS2D,
                radius, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH, true, false, durationMs
        ));
    }

    public static void addCross3D(Vec3 center, double xRadius, double yRadius, double zRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.CROSS3D,
                0, xRadius, yRadius, zRadius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH, true, false, durationMs
        ));
    }

    // -------------------- FAN --------------------

    public static void addFan2D(Vec3 center, Direction dir, double radius, double angleDeg, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.FAN2D,
                radius, 0, 0, 0,
                0, 0, 0, 0,
                angleDeg, 0, 0,
                dir, true, false, durationMs
        ));
    }

    public static void addFan3D(Vec3 center, Direction dir, double radius, double angleDeg, double yRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.FAN3D,
                radius, 0, yRadius, 0,
                0, 0, 0, 0,
                angleDeg, 0, 0,
                dir, true, false, durationMs
        ));
    }

    // -------------------- TRIANGLE --------------------

    public static void addTriangle2D(Vec3 center, Direction dir, double base, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.TRIANGLE2D,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, base, 0,
                dir, true, false, durationMs
        ));
    }

    public static void addTriangle3D(Vec3 center, Direction dir, double base, double yRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.TRIANGLE3D,
                0, 0, yRadius, 0,
                0, 0, 0, 0,
                0, base, 0,
                dir, true, false, durationMs
        ));
    }

    // -------------------- RANDOM --------------------

    public static void addRandom(Vec3 center, double xRange, double yRange, double zRange,
                                 int count, int sizeX, int sizeY, int sizeZ, boolean use3D, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.RANDOM,
                0, xRange, yRange, zRange,
                count, sizeX, sizeY, sizeZ,
                0, 0, 0,
                Direction.NORTH, true, use3D, durationMs
        ));
    }
// -------------------- SPHERE --------------------
    /**
     * 球体 AoE を追加
     * @param center 中心座標
     * @param radius 半径
     * @param includeCenter 中心を含めるか
     * @param durationMs 表示時間（ms）
     */
    public static void addSphere(Vec3 center, double radius, boolean includeCenter, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, Shape.SPHERE,
                radius, 0, 0, 0,        // x/y/z 半径は不要（AttackArea.getSphere は radius のみ）
                0, 0, 0, 0,             // ランダム関係
                0, 0, 0,                // 扇形・三角形関連
                null,                    // 方向は不要
                includeCenter,
                false,                   // use3D は不要
                durationMs
        ));
    }
}
