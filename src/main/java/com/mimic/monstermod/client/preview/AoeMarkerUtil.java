package com.mimic.monstermod.client.preview;

import com.mimic.monstermod.client.preview.AoeMarkerManager.AoeMarker;
import com.mimic.monstermod.client.preview.AoeMarkerManager.Shape;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * AoEマーカー追加用ユーティリティクラス
 * 全形状対応。
 */
public class AoeMarkerUtil {

    // -------------------- CIRCLE --------------------

    public static void addCircle2D(Vec3 center, int radius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.CIRCLE2D,
                radius,
                0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH,
                true,
                false,
                durationMs
        ));
    }

    public static void addCircle3D(Vec3 center, int radius, int yRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.CIRCLE3D,
                radius,
                radius, yRadius, radius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH,
                true,
                false,
                durationMs
        ));
    }

    // -------------------- RECT --------------------

    public static void addRect2D(Vec3 center, int xRadius, int zRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.RECT2D,
                0,
                xRadius, 0, zRadius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH,
                true,
                false,
                durationMs
        ));
    }

    public static void addRect3D(Vec3 center, int xRadius, int yRadius, int zRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.RECT3D,
                0,
                xRadius, yRadius, zRadius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH,
                true,
                false,
                durationMs
        ));
    }

    // -------------------- CROSS --------------------

    public static void addCross2D(Vec3 center, int radius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.CROSS2D,
                radius,
                0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH,
                true,
                false,
                durationMs
        ));
    }

    public static void addCross3D(Vec3 center, int xRadius, int yRadius, int zRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.CROSS3D,
                0,
                xRadius, yRadius, zRadius,
                0, 0, 0, 0,
                0, 0, 0,
                Direction.NORTH,
                true,
                false,
                durationMs
        ));
    }

    // -------------------- FAN --------------------

    public static void addFan2D(Vec3 center, Direction dir, int radius, double angleDeg, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.FAN2D,
                radius,
                0, 0, 0,
                0, 0, 0, 0,
                angleDeg,
                0, 0,
                dir,
                true,
                false,
                durationMs
        ));
    }

    public static void addFan3D(Vec3 center, Direction dir, int radius, double angleDeg, int yRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.FAN3D,
                radius,
                0, yRadius, 0,
                0, 0, 0, 0,
                angleDeg,
                0, 0,
                dir,
                true,
                false,
                durationMs
        ));
    }

    // -------------------- TRIANGLE --------------------

    public static void addTriangle2D(Vec3 center, Direction dir, int base, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.TRIANGLE2D,
                0,
                0, 0, 0,
                0, 0, 0, 0,
                0,
                base, 0,
                dir,
                true,
                false,
                durationMs
        ));
    }

    public static void addTriangle3D(Vec3 center, Direction dir, int base, int yRadius, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.TRIANGLE3D,
                0,
                0, yRadius, 0,
                0, 0, 0, 0,
                0,
                base, 0,
                dir,
                true,
                false,
                durationMs
        ));
    }

    // -------------------- RANDOM --------------------

    public static void addRandom(Vec3 center, int xRange, int yRange, int zRange, int count, int sizeX, int sizeY, int sizeZ, boolean use3D, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center,
                Shape.RANDOM,
                0,
                xRange, yRange, zRange,
                count, sizeX, sizeY, sizeZ,
                0,
                0, 0,
                Direction.NORTH,
                true,
                use3D,
                durationMs
        ));
    }
}
