package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AttackPreview2DBlockMath;
import com.mimic.monstermod.Math.AttackPreview2DMath;
import com.mimic.monstermod.Math.AttackPreview3DMath;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class AoeMarkerManager {

    // -------------------- マーカー形状 --------------------
    public enum Shape {
        // 3D
        BOX, SPHERE, CYLINDER, CAPSULE, FAN3D, CROSS3D, TRIANGLE_PRISM,
        // Block2D
        CIRCLE_BLOCK2D, FAN_BLOCK2D, TRIANGLE_BLOCK2D, RECT_BLOCK2D, CROSS_BLOCK2D, RANDOM_BLOCK2D,
        // 2D描画
        CIRCLE_2D, FAN_2D, TRIANGLE_2D, RECT_2D, CROSS_2D, RANDOM_2D
    }

    private static final List<AoeMarker> MARKERS = new ArrayList<>();
    private static final int DEFAULT_SEGMENTS = 24;

    // -------------------- マーカー管理 --------------------
    public static void addMarker(AoeMarker marker) { MARKERS.add(marker); }
    public static List<AoeMarker> getMarkers() { return MARKERS; }
    public static void updateMarkers() {
        long now = System.currentTimeMillis();
        MARKERS.removeIf(m -> m.endTime < now);
    }

    // -------------------- マーカー本体 --------------------
    public static class AoeMarker {

        public final Vec3 center;
        public final Shape shape;
        public final Level level;
        public final int segments;

        public final double radius, xRadius, yRadius, zRadius, height, base, angleDeg;
        public final Direction direction;

        private final long durationMs;
        public final long endTime;

        public AoeMarker(Vec3 center, Shape shape,
                         double radius, double xRadius, double yRadius, double zRadius,
                         double height, double base, double angleDeg,
                         Direction direction,
                         Level level, int segments,
                         long durationMs) {

            this.center = center;
            this.shape = shape;
            this.radius = radius;
            this.xRadius = xRadius;
            this.yRadius = yRadius;
            this.zRadius = zRadius;
            this.height = height;
            this.base = base;
            this.angleDeg = angleDeg;
            this.direction = direction;
            this.level = level;
            this.segments = segments;
            this.durationMs = durationMs;
            this.endTime = System.currentTimeMillis() + durationMs;
        }

        public float getProgress() {
            long now = System.currentTimeMillis();
            long elapsed = durationMs - Math.max(0, endTime - now);
            return Math.max(0f, Math.min(1f, elapsed / (float) durationMs));
        }

        // -------------------- 3D描画用 --------------------
        public AttackPreview3DMath.ShapeData get3DShapeData() {
            // ShapeDataを生成する際に、座標がnullやワールド外にならないように補正
            Vec3 safeCenter = new Vec3(
                    center.x,
                    Math.max(center.y, 0.1), // Y座標は最低0.1に補正
                    center.z
            );

            return switch (shape) {
                case BOX -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.BOX, safeCenter,
                        xRadius * 2, yRadius * 2, zRadius * 2, 0, 0, 0, 0, 0, 0
                );
                case SPHERE -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.SPHERE, safeCenter,
                        0, 0, 0, radius, 0, 24, 12, 0, 0
                );
                case CYLINDER -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.CYLINDER, safeCenter,
                        0, 0, 0, radius, height, 24, 1, 0, 0
                );
                case CAPSULE -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.CAPSULE, safeCenter,
                        0, 0, 0, radius, height, 24, 1, 8, 0
                );
                case FAN3D -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.FAN3D, safeCenter,
                        0, 0, 0, radius, height, 24, 0, 0, angleDeg
                );
                case CROSS3D -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.CROSS3D, safeCenter,
                        xRadius * 2, yRadius * 2, zRadius * 2, 0, 0, 0, 0, 0, 0
                );
                case TRIANGLE_PRISM -> AttackPreview3DMath.getShapeData(
                        AttackPreview3DMath.ShapeType.TRIANGLE_PRISM, safeCenter,
                        base, height, zRadius, 0, 0, 0, 0, 0, 0
                );
                default -> null;
            };
        }

        // -------------------- Block2D描画用 --------------------
        public AttackPreview2DBlockMath.Area2DBlock getBlock2DArea() {
            if (level == null) return null;
            return switch (shape) {
                case CIRCLE_BLOCK2D -> AttackPreview2DBlockMath.generateAreaWithOutline(level,
                        AttackPreview2DBlockMath.ShapeType.CIRCLEBlock2D, center, radius, xRadius, zRadius, base, direction,
                        center.y - yRadius, center.y + yRadius, segments);
                case FAN_BLOCK2D -> AttackPreview2DBlockMath.generateAreaWithOutline(level,
                        AttackPreview2DBlockMath.ShapeType.FANBlock2D, center, radius, xRadius, zRadius, base, direction,
                        center.y - yRadius, center.y + yRadius, segments);
                case TRIANGLE_BLOCK2D -> AttackPreview2DBlockMath.generateAreaWithOutline(level,
                        AttackPreview2DBlockMath.ShapeType.TRIANGLEBlock2D, center, radius, xRadius, zRadius, base, direction,
                        center.y - yRadius, center.y + yRadius, segments);
                case RECT_BLOCK2D -> AttackPreview2DBlockMath.generateAreaWithOutline(level,
                        AttackPreview2DBlockMath.ShapeType.RECTBlock2D, center, radius, xRadius, zRadius, base, direction,
                        center.y - yRadius, center.y + yRadius, segments);
                case CROSS_BLOCK2D -> AttackPreview2DBlockMath.generateAreaWithOutline(level,
                        AttackPreview2DBlockMath.ShapeType.CROSSBlock2D, center, radius, xRadius, zRadius, base, direction,
                        center.y - yRadius, center.y + yRadius, segments);
                case RANDOM_BLOCK2D -> AttackPreview2DBlockMath.generateAreaWithOutline(level,
                        AttackPreview2DBlockMath.ShapeType.RANDOMBlock2D, center, radius, xRadius, zRadius, base, direction,
                        center.y - yRadius, center.y + yRadius, segments);
                default -> null;
            };
        }

        // -------------------- 2D描画用 --------------------
        public List<Vec3[]> get2DShapePoints() {
            if (level == null) return null;
            return switch (shape) {
                case CIRCLE_2D -> AttackPreview2DMath.getShapePoints(level, AttackPreview2DMath.ShapeType.CIRCLE2D,
                        center, radius, base, angleDeg, segments);
                case FAN_2D -> AttackPreview2DMath.getShapePoints(level, AttackPreview2DMath.ShapeType.FAN2D,
                        center, radius, base, angleDeg, segments);
                case TRIANGLE_2D -> AttackPreview2DMath.getShapePoints(level, AttackPreview2DMath.ShapeType.TRIANGLE2D,
                        center, radius, base, angleDeg, segments);
                case RECT_2D -> AttackPreview2DMath.getShapePoints(level, AttackPreview2DMath.ShapeType.RECT2D,
                        center, radius, base, angleDeg, segments);
                case CROSS_2D -> AttackPreview2DMath.getShapePoints(level, AttackPreview2DMath.ShapeType.CROSS2D,
                        center, radius, base, angleDeg, segments);
                case RANDOM_2D -> AttackPreview2DMath.getShapePoints(level, AttackPreview2DMath.ShapeType.RANDOM2D,
                        center, radius, base, angleDeg, segments);
                default -> null;
            };
        }
    }
}
