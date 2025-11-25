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

    /** 対応形状列挙 */
    public enum Shape {
        CIRCLE2D, CIRCLE3D,
        RECT2D, RECT3D,
        CROSS2D, CROSS3D,
        FAN2D, FAN3D,
        TRIANGLE2D, TRIANGLE3D,
        RANDOM,
        SPHERE
    }

    /** 登録済みマーカーリスト */
    private static final List<AoeMarker> MARKERS = new ArrayList<>();

    /** マーカー追加 */
    public static void addMarker(AoeMarker marker) {
        MARKERS.add(marker);
    }

    /** マーカー取得 */
    public static List<AoeMarker> getMarkers() {
        return MARKERS;
    }

    /** AoEマーカー情報クラス */
    public static class AoeMarker {

        // 中心座標（Minecraft Vec3）
        public final Vec3 center;
        // マーカー形状
        public final Shape shape;

        // 基本形状半径（double に変更して AttackArea に対応）
        public final double radius;
        public final double xRadius;
        public final double yRadius;
        public final double zRadius;

        // ランダム生成関連
        public final int count;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;

        // 扇形・三角形関連
        public final double angleDeg;
        public final double base;           // 三角形底辺
        public final double height;         // 三角形高さ
        public final Direction direction;
        public final boolean includeCenter;

        // ランダム3D制御
        public final boolean use3D;

        // 寿命管理
        public final long endTime;
        private final long durationMs;

        /**
         * AoEマーカー生成
         * @param center 中心座標
         * @param shape 形状
         * @param radius 半径
         * @param xRadius X方向半径
         * @param yRadius Y方向半径
         * @param zRadius Z方向半径
         * @param count ランダム生成数
         * @param sizeX ランダムサイズX
         * @param sizeY ランダムサイズY
         * @param sizeZ ランダムサイズZ
         * @param angleDeg 扇形角度
         * @param base 三角形底辺
         * @param height 三角形高さ
         * @param direction 方向
         * @param includeCenter 中心含めるか
         * @param use3D ランダム3Dか
         * @param durationMs 表示時間（ms）
         */
        public AoeMarker(
                Vec3 center,
                Shape shape,
                double radius,
                double xRadius, double yRadius, double zRadius,
                int count, int sizeX, int sizeY, int sizeZ,
                double angleDeg,
                double base, double height,
                Direction direction,
                boolean includeCenter,
                boolean use3D,
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
            this.base = base;
            this.height = height;
            this.direction = direction;
            this.includeCenter = includeCenter;

            this.use3D = use3D;

            this.durationMs = durationMs;
            this.endTime = System.currentTimeMillis() + durationMs;
        }

        /** マーカー進行度取得（0〜1） */
        public float getProgress() {
            long now = System.currentTimeMillis();
            long elapsed = durationMs - Math.max(0, endTime - now);
            return Math.max(0f, Math.min(1f, elapsed / (float) durationMs));
        }
    }

    /** マーカー更新（寿命切れ削除） */
    public static void updateMarkers() {
        long now = System.currentTimeMillis();
        MARKERS.removeIf(m -> m.endTime < now);
    }
}
