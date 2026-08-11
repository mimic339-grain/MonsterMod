package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * MeshBlockConverter（プレイヤー距離優先完全版）
 *
 * ・Mesh → BlockPos量子化
 * ・XZごとに1ブロック以上
 * ・Y方向はプレイヤーに近いBlockを優先
 * ・同距離なら複数Quad生成
 */
public final class MeshBlockConverter {

    private MeshBlockConverter() {}

    public static List<BlockPos> toBlocks(MathMain math, Level level, Vec3 playerPos) {
        // このパスはrenderBlock2D(ブロック設置系プレビュー)専用で、Radial/Spiral系は通らないためlead不要
        AoeMeshBuilder2D builder = new AoeMeshBuilder2D(null, math);
        List<AoeMeshBuilder2D.Quad> quads = builder.build();

        // ★ List ではなく Set を使って重複を自動排除する
        java.util.Set<BlockPos> resultSet = new java.util.HashSet<>();

        float r = Math.max(Math.max(math.radius, math.xRadius),
                Math.max(math.zRadius, math.depth));

        int minX = (int)Math.floor(math.origin.x - r - 1);
        int maxX = (int)Math.ceil (math.origin.x + r + 1);
        int minZ = (int)Math.floor(math.origin.z - r - 1);
        int maxZ = (int)Math.ceil (math.origin.z + r + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                List<BlockPos> candidates =
                        findNearbyPlaceableBlocks(quads, x, z, level, playerPos);
                // ★ Set に追加（同じ座標なら無視される）
                resultSet.addAll(candidates);
            }
        }

        // 最後に List に戻して返す
        return new ArrayList<>(resultSet);
    }
    /**
     * プレイヤーに近い順にBlockPos候補を返す
     * 同距離で複数あれば両方返す
     */
    private static List<BlockPos> findNearbyPlaceableBlocks(
            List<AoeMeshBuilder2D.Quad> quads,
            int x,
            int z,
            Level level,
            Vec3 playerPos
    ) {
        List<BlockPos> candidates = new ArrayList<>();

        int playerY = (int)Math.floor(playerPos.y);

        int minY = Math.max(level.getMinBuildHeight(), playerY - 10);
        int maxY = Math.min(level.getMaxBuildHeight(), playerY + 10);

        double closestDistSq = Double.MAX_VALUE;

        for (int offset = 0; offset <= Math.max(maxY - playerY, playerY - minY); offset++) {
            // 上方向
            int yUp = playerY + offset;
            if (yUp < maxY) {
                BlockPos pos = new BlockPos(x, yUp, z);
                if (isPlaceable(quads, pos, level)) {
                    double dist = playerPos.distanceToSqr(x + 0.5, yUp + 0.5, z + 0.5);
                    if (dist < closestDistSq) {
                        candidates.clear();
                        candidates.add(pos);
                        closestDistSq = dist;
                    } else if (dist == closestDistSq) {
                        candidates.add(pos);
                    }
                }
            }
            // 下方向
            int yDown = playerY - offset;
            if (yDown >= minY) {
                BlockPos pos = new BlockPos(x, yDown, z);
                if (isPlaceable(quads, pos, level)) {
                    double dist = playerPos.distanceToSqr(x + 0.5, yDown + 0.5, z + 0.5);
                    if (dist < closestDistSq) {
                        candidates.clear();
                        candidates.add(pos);
                        closestDistSq = dist;
                    } else if (dist == closestDistSq) {
                        candidates.add(pos);
                    }
                }
            }
        }

        return candidates;
    }

    /**
     * 下がsolid、上が空気でMesh内なら立てられるBlockPos
     */
    private static boolean isPlaceable(List<AoeMeshBuilder2D.Quad> quads,
                                       BlockPos pos, Level level) {
        BlockState below = level.getBlockState(pos.below());
        BlockState here  = level.getBlockState(pos);
        //ここ上が空気じゃないとだめっていうのは半ブロック雪に邪魔される
        /*if (!below.isSolidRender(level, pos.below()) || !here.isAir()) return false;*/

        Vec3 sample = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return insideAnyQuad(sample, quads);
    }

    private static boolean insideAnyQuad(Vec3 p, List<AoeMeshBuilder2D.Quad> quads) {
        for (AoeMeshBuilder2D.Quad q : quads) {
            Vec3[] v = q.pos();
            if (pointInTriangle(p, v[0], v[1], v[2])) return true;
            if (pointInTriangle(p, v[0], v[2], v[3])) return true;
        }
        return false;
    }

    private static boolean pointInTriangle(Vec3 p, Vec3 a, Vec3 b, Vec3 c) {
        double px = p.x, pz = p.z;
        double ax = a.x, az = a.z;
        double bx = b.x, bz = b.z;
        double cx = c.x, cz = c.z;

        double ab = cross(ax, az, bx, bz, px, pz);
        double bc = cross(bx, bz, cx, cz, px, pz);
        double ca = cross(cx, cz, ax, az, px, pz);

        return (ab >= 0 && bc >= 0 && ca >= 0) || (ab <= 0 && bc <= 0 && ca <= 0);
    }

    private static double cross(double ax, double az,
                                double bx, double bz,
                                double px, double pz) {
        return (bx - ax) * (pz - az) - (bz - az) * (px - ax);
    }
}