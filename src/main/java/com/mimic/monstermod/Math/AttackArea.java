package com.mimic.monstermod.Math;

import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AttackArea - AoEマーカー計算ユーティリティ
 * 2D/3D全形状対応、座標はすべて外部から渡す完全版
 */
public class AttackArea {

    /** 2D円形（XZ平面） */
    public static List<int[]> getCircle2D(int centerX, int centerY, int centerZ, int radius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter) list.add(new int[]{centerX, centerY, centerZ});
        int rSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= rSq && !(dx == 0 && dz == 0 && !includeCenter))
                    list.add(new int[]{centerX + dx, centerY, centerZ + dz});
        return list;
    }

    /** 3D円柱 */
    public static List<int[]> getCircle3D(int centerX, int centerY, int centerZ, int radius, int yRadius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter)
            for (int dy = -yRadius; dy <= yRadius; dy++)
                list.add(new int[]{centerX, centerY + dy, centerZ});
        int rSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= rSq)
                    for (int dy = -yRadius; dy <= yRadius; dy++)
                        if (!(dx == 0 && dz == 0 && !includeCenter))
                            list.add(new int[]{centerX + dx, centerY + dy, centerZ + dz});
        return list;
    }

    /** 2D長方形 */
    public static List<int[]> getRect2D(int centerX, int centerY, int centerZ, int xRadius, int zRadius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter) list.add(new int[]{centerX, centerY, centerZ});
        for (int dx = -xRadius; dx <= xRadius; dx++)
            for (int dz = -zRadius; dz <= zRadius; dz++)
                if (!(dx == 0 && dz == 0 && !includeCenter))
                    list.add(new int[]{centerX + dx, centerY, centerZ + dz});
        return list;
    }

    /** 3D長方形 */
    public static List<int[]> getRect3D(int centerX, int centerY, int centerZ, int xRadius, int yRadius, int zRadius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter) list.add(new int[]{centerX, centerY, centerZ});
        for (int dx = -xRadius; dx <= xRadius; dx++)
            for (int dy = -yRadius; dy <= yRadius; dy++)
                for (int dz = -zRadius; dz <= zRadius; dz++)
                    if (!(dx == 0 && dy == 0 && dz == 0 && !includeCenter))
                        list.add(new int[]{centerX + dx, centerY + dy, centerZ + dz});
        return list;
    }

    /** 扇形2D */
    public static List<int[]> getFan2D(int centerX, int centerY, int centerZ, Direction dir, int radius, double angleDeg, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter) list.add(new int[]{centerX, centerY, centerZ});
        double halfRad = Math.toRadians(angleDeg / 2.0);
        for (int d = 1; d <= radius; d++) {
            int left = (int) Math.floor(-Math.tan(halfRad) * d);
            int right = (int) Math.ceil(Math.tan(halfRad) * d);
            for (int offset = left; offset <= right; offset++) {
                int x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + offset; z = centerZ - d; }
                    case SOUTH -> { x = centerX + offset; z = centerZ + d; }
                    case WEST -> { x = centerX - d; z = centerZ + offset; }
                    case EAST -> { x = centerX + d; z = centerZ + offset; }
                }
                list.add(new int[]{x, centerY, z});
            }
        }
        return list;
    }

    /** 扇形3D */
    public static List<int[]> getFan3D(int centerX, int centerY, int centerZ, Direction dir, int radius, double angleDeg, int yRadius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter)
            for (int dy = -yRadius; dy <= yRadius; dy++)
                list.add(new int[]{centerX, centerY + dy, centerZ});
        double halfRad = Math.toRadians(angleDeg / 2.0);
        for (int d = 1; d <= radius; d++) {
            int left = (int) Math.floor(-Math.tan(halfRad) * d);
            int right = (int) Math.ceil(Math.tan(halfRad) * d);
            for (int offset = left; offset <= right; offset++) {
                int x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + offset; z = centerZ - d; }
                    case SOUTH -> { x = centerX + offset; z = centerZ + d; }
                    case WEST -> { x = centerX - d; z = centerZ + offset; }
                    case EAST -> { x = centerX + d; z = centerZ + offset; }
                }
                for (int dy = -yRadius; dy <= yRadius; dy++)
                    list.add(new int[]{x, centerY + dy, z});
            }
        }
        return list;
    }

    /** 十字2D */
    public static List<int[]> getCross2D(int centerX, int centerY, int centerZ, int radius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter) list.add(new int[]{centerX, centerY, centerZ});
        for (int dx = -radius; dx <= radius; dx++) if (dx != 0) list.add(new int[]{centerX + dx, centerY, centerZ});
        for (int dz = -radius; dz <= radius; dz++) if (dz != 0) list.add(new int[]{centerX, centerY, centerZ + dz});
        return list;
    }

    /** 十字3D */
    public static List<int[]> getCross3D(int centerX, int centerY, int centerZ, int xRadius, int yRadius, int zRadius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        for (int dx = -xRadius; dx <= xRadius; dx++)
            for (int dy = -yRadius; dy <= yRadius; dy++)
                if (dx != 0 || dy != 0 || includeCenter) list.add(new int[]{centerX + dx, centerY + dy, centerZ});
        for (int dz = -zRadius; dz <= zRadius; dz++)
            for (int dy = -yRadius; dy <= yRadius; dy++)
                if (dz != 0 || dy != 0 || includeCenter) list.add(new int[]{centerX, centerY + dy, centerZ + dz});
        return list;
    }

    /** 三角形2D */
    public static List<int[]> getTriangle2D(int centerX, int centerY, int centerZ, int base, Direction dir, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter) list.add(new int[]{centerX, centerY, centerZ});
        for (int h = 1; h <= base; h++) {
            int half = (int) Math.floor((double) base * h / base / 2.0);
            for (int o = -half; o <= half; o++) {
                int x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + o; z = centerZ - h; }
                    case SOUTH -> { x = centerX + o; z = centerZ + h; }
                    case WEST -> { x = centerX - h; z = centerZ + o; }
                    case EAST -> { x = centerX + h; z = centerZ + o; }
                }
                list.add(new int[]{x, centerY, z});
            }
        }
        return list;
    }

    /** 三角形3D */
    public static List<int[]> getTriangle3D(int centerX, int centerY, int centerZ, int base, Direction dir, int yRadius, boolean includeCenter) {
        List<int[]> list = new ArrayList<>();
        if (includeCenter)
            for (int dy = -yRadius; dy <= yRadius; dy++)
                list.add(new int[]{centerX, centerY + dy, centerZ});
        for (int h = 1; h <= base; h++) {
            int half = (int) Math.floor((double) base * h / base / 2.0);
            for (int o = -half; o <= half; o++) {
                int x = centerX, z = centerZ;
                switch (dir) {
                    case NORTH -> { x = centerX + o; z = centerZ - h; }
                    case SOUTH -> { x = centerX + o; z = centerZ + h; }
                    case WEST -> { x = centerX - h; z = centerZ + o; }
                    case EAST -> { x = centerX + h; z = centerZ + o; }
                }
                for (int dy = -yRadius; dy <= yRadius; dy++)
                    list.add(new int[]{x, centerY + dy, z});
            }
        }
        return list;
    }

    /** ランダムポイント */
    public static List<int[]> getRandomPoints(int centerX, int centerY, int centerZ, int xRange, int yRange, int zRange, int count, int sizeX, int sizeY, int sizeZ, boolean use3D) {
        List<int[]> list = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int x = centerX - xRange + rand.nextInt(xRange * 2 + 1);
            int y = centerY - yRange + (use3D ? rand.nextInt(yRange * 2 + 1) : 0);
            int z = centerZ - zRange + rand.nextInt(zRange * 2 + 1);
            for (int sx = 0; sx < sizeX; sx++)
                for (int sy = 0; sy < sizeY; sy++)
                    for (int sz = 0; sz < sizeZ; sz++)
                        list.add(new int[]{x + sx, y + (use3D ? sy : 0), z + sz});
        }
        return list;
    }
}
