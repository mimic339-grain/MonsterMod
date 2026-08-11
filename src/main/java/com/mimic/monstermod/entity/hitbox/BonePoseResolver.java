package com.mimic.monstermod.entity.hitbox;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * BoneRigDataの静的データ + 「今どのアニメーションが何秒再生されているか」から、
 * 各hitbox_*ボーンのワールド座標系での回転済み当たり判定箱を計算する。
 *
 * 【重要】ここの変換順序は GeckoLib の以下と完全に一致させてある。
 * ズレると描画と当たり判定がズレるため、勝手に単純化・最適化しないこと。
 *   RenderUtils#prepMatrixForBone :
 *      translate(-posX/16, posY/16, posZ/16)   ← アニメの位置(Xのみ反転)
 *      translate(pivot/16)
 *      rotateZ → rotateY → rotateX             ← 基本回転 + アニメ回転
 *      translate(-pivot/16)
 *   GeoRenderer#renderCube :
 *      translate(cubePivot/16) → rotateZ,Y,X → translate(-cubePivot/16)
 *   GeoEntityRenderer#applyRotations :
 *      mulPose(Axis.YP.rotationDegrees(180 - yaw))
 *
 * クライアント・サーバー双方が同じ入力(アニメーション名・経過秒・yaw・座標)から
 * 同じ結果を得る決定的な計算なので、ネットワーク同期は不要。
 */
public final class BonePoseResolver {

    private BonePoseResolver() {}

    /**
     * 指定ボーンの、ワールド座標系での回転済みヒットボックスの8頂点を返す。
     * ヒットボックス定義が無いボーンの場合は null。
     */
    public static Vector3f[] resolveWorldCorners(BoneRigData rig, String hitboxBoneName,
                                                 String animation, double animTimeSeconds,
                                                 Vec3 entityPos, float entityYaw) {
        BoneRigData.BoneCube cube = rig.getHitboxCube(hitboxBoneName);
        if (cube == null) return null;

        Matrix4f m = new Matrix4f();

        // GeoEntityRenderer#applyRotations 相当(モデル全体の向き)
        m.rotateY((float) Math.toRadians(180.0f - entityYaw));

        // 根から順に prepMatrixForBone 相当を適用
        for (String boneName : rig.resolveParentChain(hitboxBoneName)) {
            BoneRigData.BoneDef bone = rig.getBone(boneName);
            if (bone == null) continue;

            Vector3f pivot = bone.pivot(); // ピクセル単位・X反転済み
            Vector3f animPos = rig.samplePositionPixels(animation, boneName, animTimeSeconds);
            Vector3f animRot = rig.sampleRotationRad(animation, boneName, animTimeSeconds);

            // translateMatrixToBone: Xのみ反転して1/16
            m.translate(-animPos.x / 16f, animPos.y / 16f, animPos.z / 16f);

            m.translate(pivot.x / 16f, pivot.y / 16f, pivot.z / 16f);

            // 基本回転(geo.json) + アニメーション回転。回転順は Z → Y → X
            float rotX = bone.baseRotationRad().x + animRot.x;
            float rotY = bone.baseRotationRad().y + animRot.y;
            float rotZ = bone.baseRotationRad().z + animRot.z;
            if (rotZ != 0) m.rotateZ(rotZ);
            if (rotY != 0) m.rotateY(rotY);
            if (rotX != 0) m.rotateX(rotX);

            m.translate(-pivot.x / 16f, -pivot.y / 16f, -pivot.z / 16f);
        }

        // renderCube 相当(Cube自身のpivot/回転。翼や尻尾はここに大きな回転を持つ)
        Vector3f cubePivot = cube.pivot();
        m.translate(cubePivot.x / 16f, cubePivot.y / 16f, cubePivot.z / 16f);
        if (cube.rotationRad().z != 0) m.rotateZ(cube.rotationRad().z);
        if (cube.rotationRad().y != 0) m.rotateY(cube.rotationRad().y);
        if (cube.rotationRad().x != 0) m.rotateX(cube.rotationRad().x);
        m.translate(-cubePivot.x / 16f, -cubePivot.y / 16f, -cubePivot.z / 16f);

        // Cubeの8頂点(origin/sizeは既にブロック単位)
        Vector3f o = cube.origin();
        Vector3f s = cube.size();
        Vector3f[] local = {
                new Vector3f(o.x, o.y, o.z),
                new Vector3f(o.x + s.x, o.y, o.z),
                new Vector3f(o.x, o.y + s.y, o.z),
                new Vector3f(o.x + s.x, o.y + s.y, o.z),
                new Vector3f(o.x, o.y, o.z + s.z),
                new Vector3f(o.x + s.x, o.y, o.z + s.z),
                new Vector3f(o.x, o.y + s.y, o.z + s.z),
                new Vector3f(o.x + s.x, o.y + s.y, o.z + s.z),
        };

        Vector3f[] world = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector4f v = m.transform(new Vector4f(local[i], 1.0f));
            world[i] = new Vector3f(
                    (float) (entityPos.x + v.x),
                    (float) (entityPos.y + v.y),
                    (float) (entityPos.z + v.z));
        }
        return world;
    }

    /** エンティティ基準のローカル座標(描画用。poseStackが既にエンティティ位置にある前提) */
    public static Vector3f[] resolveLocalCorners(BoneRigData rig, String hitboxBoneName,
                                                 String animation, double animTimeSeconds, float entityYaw) {
        Vector3f[] world = resolveWorldCorners(rig, hitboxBoneName, animation, animTimeSeconds, Vec3.ZERO, entityYaw);
        return world;
    }

    /** 8頂点を包含する最小のAABB(当たり判定の実体として使う軽量版) */
    public static AABB enclosingAABB(Vector3f[] corners) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Vector3f c : corners) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** OBBの12本の辺(頂点インデックスのペア)。resolveWorldCornersの頂点順に対応。 */
    public static final int[][] EDGES = {
            {0, 1}, {0, 2}, {1, 3}, {2, 3},
            {4, 5}, {4, 6}, {5, 7}, {6, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
}
