package com.mimic.monstermod.Math;

import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AttackArea - AoEマーカー計算ユーティリティ
 * 2D/3D全形状対応、座標はすべて double で扱う完全版
 */
public class AttackArea {

    /** 2D円形（XZ平面） */
    public static List<double[]> getCircle2D(double centerX, double centerY, double centerZ, double radius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});
        double rSq = radius * radius;
        for (double dx = -radius; dx <= radius; dx++)
            for (double dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= rSq && !(dx == 0 && dz == 0 && !includeCenter))
                    list.add(new double[]{centerX + dx, centerY, centerZ + dz});
        return list;
    }

    /** 3D円柱 */
    public static List<double[]> getCircle3D(double centerX, double centerY, double centerZ, double radius, double yRadius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter)
            for (double dy = -yRadius; dy <= yRadius; dy++)
                list.add(new double[]{centerX, centerY + dy, centerZ});
        double rSq = radius * radius;
        for (double dx = -radius; dx <= radius; dx++)
            for (double dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= rSq)
                    for (double dy = -yRadius; dy <= yRadius; dy++)
                        if (!(dx == 0 && dz == 0 && !includeCenter))
                            list.add(new double[]{centerX + dx, centerY + dy, centerZ + dz});
        return list;
    }

    /** 2D長方形 */
    public static List<double[]> getRect2D(double centerX, double centerY, double centerZ, double xRadius, double zRadius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});
        for (double dx = -xRadius; dx <= xRadius; dx++)
            for (double dz = -zRadius; dz <= zRadius; dz++)
                if (!(dx == 0 && dz == 0 && !includeCenter))
                    list.add(new double[]{centerX + dx, centerY, centerZ + dz});
        return list;
    }

    /** 3D長方形 */
    public static List<double[]> getRect3D(double centerX, double centerY, double centerZ, double xRadius, double yRadius, double zRadius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});
        for (double dx = -xRadius; dx <= xRadius; dx++)
            for (double dy = -yRadius; dy <= yRadius; dy++)
                for (double dz = -zRadius; dz <= zRadius; dz++)
                    if (!(dx == 0 && dy == 0 && dz == 0 && !includeCenter))
                        list.add(new double[]{centerX + dx, centerY + dy, centerZ + dz});
        return list;
    }

    /** 扇形2D */
    public static List<double[]> getFan2D(double centerX, double centerY, double centerZ, Direction dir, double radius, double angleDeg, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});
        double halfRad = Math.toRadians(angleDeg / 2.0);
        for (double d = 1; d <= radius; d++) {
            int left = (int) Math.floor(-Math.tan(halfRad) * d);
            int right = (int) Math.ceil(Math.tan(halfRad) * d);
            for (int offset = left; offset <= right; offset++) {
                double x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + offset; z = centerZ - d; }
                    case SOUTH -> { x = centerX + offset; z = centerZ + d; }
                    case WEST -> { x = centerX - d; z = centerZ + offset; }
                    case EAST -> { x = centerX + d; z = centerZ + offset; }
                }
                list.add(new double[]{x, centerY, z});
            }
        }
        return list;
    }

    /** 扇形3D */
    public static List<double[]> getFan3D(double centerX, double centerY, double centerZ, Direction dir, double radius, double angleDeg, double yRadius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter)
            for (double dy = -yRadius; dy <= yRadius; dy++)
                list.add(new double[]{centerX, centerY + dy, centerZ});
        double halfRad = Math.toRadians(angleDeg / 2.0);
        for (double d = 1; d <= radius; d++) {
            int left = (int) Math.floor(-Math.tan(halfRad) * d);
            int right = (int) Math.ceil(Math.tan(halfRad) * d);
            for (int offset = left; offset <= right; offset++) {
                double x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + offset; z = centerZ - d; }
                    case SOUTH -> { x = centerX + offset; z = centerZ + d; }
                    case WEST -> { x = centerX - d; z = centerZ + offset; }
                    case EAST -> { x = centerX + d; z = centerZ + offset; }
                }
                for (double dy = -yRadius; dy <= yRadius; dy++)
                    list.add(new double[]{x, centerY + dy, z});
            }
        }
        return list;
    }

    /** 十字2D */
    public static List<double[]> getCross2D(double centerX, double centerY, double centerZ, double radius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});
        for (double dx = -radius; dx <= radius; dx++) if (dx != 0) list.add(new double[]{centerX + dx, centerY, centerZ});
        for (double dz = -radius; dz <= radius; dz++) if (dz != 0) list.add(new double[]{centerX, centerY, centerZ + dz});
        return list;
    }

    /** 十字3D */
    public static List<double[]> getCross3D(double centerX, double centerY, double centerZ, double xRadius, double yRadius, double zRadius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        for (double dx = -xRadius; dx <= xRadius; dx++)
            for (double dy = -yRadius; dy <= yRadius; dy++)
                if (dx != 0 || dy != 0 || includeCenter) list.add(new double[]{centerX + dx, centerY + dy, centerZ});
        for (double dz = -zRadius; dz <= zRadius; dz++)
            for (double dy = -yRadius; dy <= yRadius; dy++)
                if (dz != 0 || dy != 0 || includeCenter) list.add(new double[]{centerX, centerY + dy, centerZ + dz});
        return list;
    }

    /** 三角形2D */
    public static List<double[]> getTriangle2D(double centerX, double centerY, double centerZ, double base, Direction dir, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});
        for (double h = 1; h <= base; h++) {
            double half = Math.floor(base * h / base / 2.0);
            for (double o = -half; o <= half; o++) {
                double x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + o; z = centerZ - h; }
                    case SOUTH -> { x = centerX + o; z = centerZ + h; }
                    case WEST -> { x = centerX - h; z = centerZ + o; }
                    case EAST -> { x = centerX + h; z = centerZ + o; }
                }
                list.add(new double[]{x, centerY, z});
            }
        }
        return list;
    }

    /** 三角形3D */
    public static List<double[]> getTriangle3D(double centerX, double centerY, double centerZ, double base, Direction dir, double yRadius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter)
            for (double dy = -yRadius; dy <= yRadius; dy++)
                list.add(new double[]{centerX, centerY + dy, centerZ});
        for (double h = 1; h <= base; h++) {
            double half = Math.floor(base * h / base / 2.0);
            for (double o = -half; o <= half; o++) {
                double x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + o; z = centerZ - h; }
                    case SOUTH -> { x = centerX + o; z = centerZ + h; }
                    case WEST -> { x = centerX - h; z = centerZ + o; }
                    case EAST -> { x = centerX + h; z = centerZ + o; }
                }
                for (double dy = -yRadius; dy <= yRadius; dy++)
                    list.add(new double[]{x, centerY + dy, z});
            }
        }
        return list;
    }

    /** ランダムポイント */
    public static List<double[]> getRandomPoints(double centerX, double centerY, double centerZ, double xRange, double yRange, double zRange, int count, double sizeX, double sizeY, double sizeZ, boolean use3D) {
        List<double[]> list = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            double x = centerX - xRange + rand.nextDouble() * xRange * 2;
            double y = centerY - yRange + (use3D ? rand.nextDouble() * yRange * 2 : 0);
            double z = centerZ - zRange + rand.nextDouble() * zRange * 2;
            for (double sx = 0; sx < sizeX; sx++)
                for (double sy = 0; sy < sizeY; sy++)
                    for (double sz = 0; sz < sizeZ; sz++)
                        list.add(new double[]{x + sx, y + (use3D ? sy : 0), z + sz});
        }
        return list;
    }
    /** 完全な球体 */
    public static List<double[]> getSphere(double centerX, double centerY, double centerZ, double radius, boolean includeCenter) {
        List<double[]> list = new ArrayList<>();
        if (includeCenter) list.add(new double[]{centerX, centerY, centerZ});

        double rSq = radius * radius;
        for (double dx = -radius; dx <= radius; dx++) {
            for (double dy = -radius; dy <= radius; dy++) {
                for (double dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        if (!(dx == 0 && dy == 0 && dz == 0 && !includeCenter)) {
                            list.add(new double[]{centerX + dx, centerY + dy, centerZ + dz});
                        }
                    }
                }
            }
        }
        return list;
    }
}
