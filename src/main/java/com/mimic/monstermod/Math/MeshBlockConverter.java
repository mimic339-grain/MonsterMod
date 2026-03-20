package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * MeshBlockConverter（完成版）
 *
 * ✔ Mesh → Block 量子化
 * ✔ Quad完全対応（2三角）
 * ✔ 4点サンプリング（高精度）
 * ✔ 動的範囲
 */
public final class MeshBlockConverter {

    private MeshBlockConverter() {}

    public static List<BlockPos> toBlocks(MathMain math, Level level) {

        AoeMeshBuilder2D builder = new AoeMeshBuilder2D(math);
        List<AoeMeshBuilder2D.Quad> quads = builder.build();

        List<BlockPos> result = new ArrayList<>();

        float r = Math.max(
                Math.max(math.radius, math.xRadius),
                Math.max(math.zRadius, math.depth)
        );

        int minX = (int)Math.floor(math.origin.x - r - 1);
        int maxX = (int)Math.ceil (math.origin.x + r + 1);
        int minZ = (int)Math.floor(math.origin.z - r - 1);
        int maxZ = (int)Math.ceil (math.origin.z + r + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                int yBlock = level.getHeight(
                        Heightmap.Types.WORLD_SURFACE,
                        x, z
                );

                double ySample = yBlock + 0.1;

                if (blockHit(quads, x, z, ySample)) {
                    result.add(new BlockPos(x, yBlock, z));
                }
            }
        }

        return result;
    }

    /* ========================= */

    private static boolean blockHit(
            List<AoeMeshBuilder2D.Quad> quads,
            int x, int z,
            double y
    ) {
        // ★ 4点サンプリング（超重要）
        return insideAnyQuad(new Vec3(x+0.2, y, z+0.2), quads) ||
                insideAnyQuad(new Vec3(x+0.8, y, z+0.2), quads) ||
                insideAnyQuad(new Vec3(x+0.2, y, z+0.8), quads) ||
                insideAnyQuad(new Vec3(x+0.8, y, z+0.8), quads);
    }

    private static boolean insideAnyQuad(
            Vec3 p,
            List<AoeMeshBuilder2D.Quad> quads
    ) {
        for (AoeMeshBuilder2D.Quad q : quads) {

            Vec3[] v = q.pos();

            // ★ Quad → 2 triangle
            if (pointInTriangle(p, v[0], v[1], v[2])) return true;
            if (pointInTriangle(p, v[0], v[2], v[3])) return true;
        }
        return false;
    }

    /* ========================= */

    private static boolean pointInTriangle(
            Vec3 p,
            Vec3 a,
            Vec3 b,
            Vec3 c
    ) {
        double px = p.x;
        double pz = p.z;

        double ax = a.x, az = a.z;
        double bx = b.x, bz = b.z;
        double cx = c.x, cz = c.z;

        double ab = cross(ax, az, bx, bz, px, pz);
        double bc = cross(bx, bz, cx, cz, px, pz);
        double ca = cross(cx, cz, ax, az, px, pz);

        return (ab >= 0 && bc >= 0 && ca >= 0) ||
                (ab <= 0 && bc <= 0 && ca <= 0);
    }

    private static double cross(
            double ax, double az,
            double bx, double bz,
            double px, double pz
    ) {
        return (bx - ax) * (pz - az) - (bz - az) * (px - ax);
    }
}