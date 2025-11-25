package com.mimic.monstermod.client.preview;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * AoEマーカー管理クラス
 * AttackArea の完全対応版。
 * 2D/3Dサークル、矩形、十字、扇形、三角形、ランダム全部サポート。
 */
public class AoeMarkerManager {

    public enum Shape {
        CIRCLE2D, CIRCLE3D,
        RECT2D, RECT3D,
        CROSS2D, CROSS3D,
        FAN2D, FAN3D,
        TRIANGLE2D, TRIANGLE3D,
        RANDOM
    }

    private static final List<AoeMarker> MARKERS = new ArrayList<>();

    public static void addMarker(AoeMarker marker) {
        MARKERS.add(marker);
    }
    public static List<AoeMarker> getMarkers() {
        return MARKERS;
    }

    public static class AoeMarker {

        public final Vec3 center;
        public final Shape shape;

        // 基本形状
        public final int radius;
        public final int xRadius;
        public final int yRadius;
        public final int zRadius;

        // ランダム型
        public final int count;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;

        // 扇形・三角形
        public final double angleDeg;
        public final int base;           // 三角形底辺
        public final int height;         // 三角形高さ
        public final Direction direction;
        public final boolean includeCenter;

        // ランダム3D生成
        public final boolean use3D;      // 2D/3Dランダム制御

        // 寿命管理
        public final long endTime;
        private final long durationMs;

        public AoeMarker(
                Vec3 center,
                Shape shape,
                int radius,
                int xRadius, int yRadius, int zRadius,
                int count, int sizeX, int sizeY, int sizeZ,
                double angleDeg,
                int base, int height,           // ← 追加
                Direction direction,
                boolean includeCenter,
                boolean use3D,                  // ← 追加
                long durationMs
        ) {
            this.center = center;
            this.shape = shape;

            this.radius = radius;
            this.xRadius = xRadius;
            this.yRadius = yRadius;
            this.zRadius = zRadius;

            this.count = count;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;

            this.angleDeg = angleDeg;
            this.base = base;                 // 追加
            this.height = height;             // 追加
            this.direction = direction;
            this.includeCenter = includeCenter;

            this.use3D = use3D;               // 追加

            this.durationMs = durationMs;
            this.endTime = System.currentTimeMillis() + durationMs;
        }

        public float getProgress() {
            long now = System.currentTimeMillis();
            long elapsed = durationMs - Math.max(0, endTime - now);
            return Math.max(0f, Math.min(1f, elapsed / (float) durationMs));
        }
    }
    public static void updateMarkers() {
        long now = System.currentTimeMillis();
        MARKERS.removeIf(m -> m.endTime < now);
    }
}