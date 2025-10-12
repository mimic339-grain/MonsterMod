package com.mimic.monstermod.Math;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AttackArea {

    /** 立体円形攻撃 */
    public static List<int[]> get3DCircle(int centerX, int centerY, int centerZ, int radius) {
        List<int[]> list = new ArrayList<>();
        int rSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    int dx = x - centerX;
                    int dy = y - centerY;
                    int dz = z - centerZ;
                    if (dx*dx + dy*dy + dz*dz <= rSquared) {
                        list.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return list;
    }

    /** 立体四角形攻撃 */
    public static List<int[]> get3DRect(int centerX, int centerY, int centerZ,
                                        int xRadius, int yRadius, int zRadius) {
        List<int[]> list = new ArrayList<>();
        for (int x = centerX - xRadius; x <= centerX + xRadius; x++) {
            for (int y = centerY - yRadius; y <= centerY + yRadius; y++) {
                for (int z = centerZ - zRadius; z <= centerZ + zRadius; z++) {
                    list.add(new int[]{x, y, z});
                }
            }
        }
        return list;
    }

    /**
     * 扇形攻撃（中心角度指定・距離ごとに横幅増加・立体対応・空白なし）
     * @param centerX プレイヤーX
     * @param centerY プレイヤーY
     * @param centerZ プレイヤーZ
     * @param dir プレイヤーの向き
     * @param maxDistance 最大前方距離
     * @param angleDeg 扇形中心角度（例: 60, 120）
     * @param yRadius Y方向範囲（0=足元固定）
     */
    public static List<int[]> getFanShape(int centerX, int centerY, int centerZ,
                                          Direction dir, int maxDistance,
                                          double angleDeg, int yRadius) {
        List<int[]> list = new ArrayList<>();
        double halfAngleRad = Math.toRadians(angleDeg / 2.0);

        for (int dist = 1; dist <= maxDistance; dist++) {
            double maxOffset = Math.tan(halfAngleRad) * dist;
            int leftBound = (int) Math.floor(-maxOffset);
            int rightBound = (int) Math.ceil(maxOffset);

            for (int offset = leftBound; offset <= rightBound; offset++) {
                int x = centerX;
                int z = centerZ;

                switch (dir) {
                    case NORTH -> { x = centerX + offset; z = centerZ - dist; }
                    case SOUTH -> { x = centerX + offset; z = centerZ + dist; }
                    case WEST  -> { x = centerX - dist; z = centerZ + offset; }
                    case EAST  -> { x = centerX + dist; z = centerZ + offset; }
                    default -> {}
                }

                for (int dy = -yRadius; dy <= yRadius; dy++) {
                    list.add(new int[]{x, centerY + dy, z});
                }
            }
        }

        return list;
    }

    /**
     * ランダム攻撃ポイント（範囲内でランダム生成、立体対応）
     * @param centerX 中心X
     * @param centerY 中心Y
     * @param centerZ 中心Z
     * @param xRange X方向範囲
     * @param yRange Y方向範囲
     * @param zRange Z方向範囲
     * @param count 生成個数
     * @param sizeX 範囲Xサイズ
     * @param sizeY 範囲Yサイズ
     * @param sizeZ 範囲Zサイズ
     */
    public static List<int[]> getRandomPoints(int centerX, int centerY, int centerZ,
                                              int xRange, int yRange, int zRange,
                                              int count, int sizeX, int sizeY, int sizeZ) {
        List<int[]> list = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int x = centerX - xRange + rand.nextInt(xRange*2 + 1);
            int y = centerY - yRange + rand.nextInt(yRange*2 + 1);
            int z = centerZ - zRange + rand.nextInt(zRange*2 + 1);

            for (int sx = 0; sx < sizeX; sx++) {
                for (int sy = 0; sy < sizeY; sy++) {
                    for (int sz = 0; sz < sizeZ; sz++) {
                        list.add(new int[]{x + sx, y + sy, z + sz});
                    }
                }
            }
        }
        return list;
    }
}
