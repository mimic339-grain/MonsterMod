package com.mimic.monstermod.client.preview;

import com.mimic.monstermod.client.preview.AoeMarkerManager.AoeMarker;
import com.mimic.monstermod.client.preview.AoeMarkerManager.Shape;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AoeMarkerUtil {

    private AoeMarkerUtil() {}

    private static final int DEFAULT_SEGMENTS = 24;

    // -------------------- 2D --------------------
    public static void addCircle2D(Vec3 center, double radius, long durationMs) {
        add2D(center, Shape.CIRCLE_2D, radius, 0, 0, 0, 0, 0, 0, Direction.NORTH, durationMs);
    }

    public static void addRect2D(Vec3 center, double xRadius, double zRadius, long durationMs) {
        add2D(center, Shape.RECT_2D, 0, xRadius, 0, zRadius, 0, 0, 0, Direction.NORTH, durationMs);
    }

    public static void addCross2D(Vec3 center, double radius, long durationMs) {
        add2D(center, Shape.CROSS_2D, radius, 0, 0, 0, 0, 0, 0, Direction.NORTH, durationMs);
    }

    public static void addFan2D(Vec3 center, Direction dir, double radius, double angleDeg, long durationMs) {
        add2D(center, Shape.FAN_2D, radius, 0, 0, 0, 0, 0, angleDeg, dir, durationMs);
    }

    public static void addTriangle2D(Vec3 center, Direction dir, double base, long durationMs) {
        add2D(center, Shape.TRIANGLE_2D, 0, 0, 0, 0, 0, base, 0, dir, durationMs);
    }

    public static void addRandom2D(Vec3 center, double xRange, double zRange, long durationMs) {
        add2D(center, Shape.RANDOM_2D, 0, xRange, 0, zRange, 0, 0, 0, Direction.NORTH, durationMs);
    }

    // -------------------- Block2D --------------------
    public static void addCircleBlock2D(Vec3 center, double radius, long durationMs) {
        add2D(center, Shape.CIRCLE_BLOCK2D, radius, 0, 0, 0, 0, 0, 0, Direction.NORTH, durationMs);
    }

    public static void addRectBlock2D(Vec3 center, double xRadius, double zRadius, long durationMs) {
        add2D(center, Shape.RECT_BLOCK2D, 0, xRadius, 0, zRadius, 0, 0, 0, Direction.NORTH, durationMs);
    }

    public static void addCrossBlock2D(Vec3 center, double radius, long durationMs) {
        add2D(center, Shape.CROSS_BLOCK2D, radius, 0, 0, 0, 0, 0, 0, Direction.NORTH, durationMs);
    }

    public static void addFanBlock2D(Vec3 center, Direction dir, double radius, double angleDeg, long durationMs) {
        add2D(center, Shape.FAN_BLOCK2D, radius, 0, 0, 0, 0, 0, angleDeg, dir, durationMs);
    }

    public static void addTriangleBlock2D(Vec3 center, Direction dir, double base, long durationMs) {
        add2D(center, Shape.TRIANGLE_BLOCK2D, 0, 0, 0, 0, 0, base, 0, dir, durationMs);
    }

    public static void addRandomBlock2D(Vec3 center, double xRange, double zRange, long durationMs) {
        add2D(center, Shape.RANDOM_BLOCK2D, 0, xRange, 0, zRange, 0, 0, 0, Direction.NORTH, durationMs);
    }

    // -------------------- 3D --------------------
    public static void addSphere(Level level, Vec3 center, double radius, long durationMs) {
        add3D(center, Shape.SPHERE, radius, 0, 0, 0, 0, 0, 0, Direction.NORTH, level, durationMs);
    }

    public static void addBox(Level level, Vec3 center, double xRadius, double yRadius, double zRadius, long durationMs) {
        add3D(center, Shape.BOX, 0, xRadius, yRadius, zRadius, 0, 0, 0, Direction.NORTH, level, durationMs);
    }

    public static void addCylinder(Level level, Vec3 center, double radius, double height, long durationMs) {
        add3D(center, Shape.CYLINDER, radius, 0, 0, 0, height, 0, 0, Direction.NORTH, level, durationMs);
    }

    public static void addCapsule(Level level, Vec3 center, double radius, double height, long durationMs) {
        add3D(center, Shape.CAPSULE, radius, 0, 0, 0, height, 0, 0, Direction.NORTH, level, durationMs);
    }

    public static void addFan3D(Level level, Vec3 center, Direction dir, double radius, double angleDeg, double height, long durationMs) {
        add3D(center, Shape.FAN3D, radius, 0, 0, 0, height, 0, angleDeg, dir, level, durationMs);
    }

    public static void addCross3D(Level level, Vec3 center, double xRadius, double yRadius, double zRadius, long durationMs) {
        add3D(center, Shape.CROSS3D, 0, xRadius, yRadius, zRadius, 0, 0, 0, Direction.NORTH, level, durationMs);
    }

    public static void addTrianglePrism(Level level, Vec3 center, Direction dir, double base, double height, double zRadius, long durationMs) {
        add3D(center, Shape.TRIANGLE_PRISM, 0, base, height, zRadius, 0, 0, 0, dir, level, durationMs);
    }


    // -------------------- 共通内部処理 --------------------
    private static void add2D(Vec3 center, Shape shape,
                              double radius, double xRadius, double yRadius, double zRadius,
                              double height, double base, double angleDeg,
                              Direction dir, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, shape,
                radius, xRadius, yRadius, zRadius,
                height, base, angleDeg,
                dir, null, DEFAULT_SEGMENTS, durationMs
        ));
    }

    private static void add3D(Vec3 center, Shape shape,
                              double radius, double xRadius, double yRadius, double zRadius,
                              double height, double base, double angleDeg,
                              Direction dir, Level level, long durationMs) {
        AoeMarkerManager.addMarker(new AoeMarker(
                center, shape,
                radius, xRadius, yRadius, zRadius,
                height, base, angleDeg,
                dir, level, DEFAULT_SEGMENTS, durationMs
        ));
    }
}
