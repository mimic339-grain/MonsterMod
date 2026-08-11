package com.mimic.monstermod.entity.hitbox;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * BoneRigDataの静的なボーン構造・キーフレームデータと、エンティティの
 * 「今何のアニメーションが何秒再生されているか」から、各hitbox_*ボーンの
 * ワールド座標系での当たり判定箱(AABB)を計算する。
 *
 * GeckoLibのAnimationProcessor/RenderUtilsには依存しない
 * (専用サーバーでのクライアント専用アセットロード依存を避けるため)。
 * 回転精度は線形補間による近似(見た目の描画ほどの精度は不要なため許容)。
 */
public final class BonePoseResolver {

    private BonePoseResolver() {}

    /**
     * 指定ボーンの、現在のワールド座標系での回転済みOBBの8頂点を返す。
     * (entityの位置・yawも合成済み)
     */
    public static Vector3f[] resolveWorldCorners(BoneRigData rig, String hitboxBoneName,
                                                  String animation, double animTimeSeconds, Entity entity) {
        BoneRigData.BoneCube cube = rig.getHitboxCube(hitboxBoneName);
        if (cube == null) return null;

        List<String> chain = rig.resolveParentChain(hitboxBoneName);

        Matrix4f model = new Matrix4f(); // モデル原点基準(エンティティの足元中心)
        for (String boneName : chain) {
            Vector3f pivot = rig.getPivot(boneName);
            Vector3f rotDeg = rig.resolveRotationDeg(animation, boneName, animTimeSeconds);

            model.translate(pivot);
            if (rotDeg.z != 0) model.rotateZ((float) Math.toRadians(rotDeg.z));
            if (rotDeg.y != 0) model.rotateY((float) Math.toRadians(-rotDeg.y));
            if (rotDeg.x != 0) model.rotateX((float) Math.toRadians(-rotDeg.x));
            model.translate(-pivot.x, -pivot.y, -pivot.z);
        }

        // モデル空間(Y軸反転無し、原点=足元)でのCubeの8頂点をワールド行列で変換
        Vector3f o = cube.origin();
        Vector3f s = cube.size();
        Vector3f[] localCorners = new Vector3f[]{
                new Vector3f(o.x, o.y, o.z),
                new Vector3f(o.x + s.x, o.y, o.z),
                new Vector3f(o.x, o.y + s.y, o.z),
                new Vector3f(o.x + s.x, o.y + s.y, o.z),
                new Vector3f(o.x, o.y, o.z + s.z),
                new Vector3f(o.x + s.x, o.y, o.z + s.z),
                new Vector3f(o.x, o.y + s.y, o.z + s.z),
                new Vector3f(o.x + s.x, o.y + s.y, o.z + s.z),
        };

        float yawRad = (float) Math.toRadians(-entity.getYRot());
        Vec3 entityPos = entity.position();

        Vector3f[] worldCorners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector4f v = new Vector4f(localCorners[i], 1.0f);
            model.transform(v);

            // Blockbenchのpivot/originはモデル中心(x,z=0)基準・Y=足元基準のため、
            // エンティティのyawで水平回転させてからワールド座標に加算する
            float cos = (float) Math.cos(yawRad);
            float sin = (float) Math.sin(yawRad);
            float wx = v.x * cos - v.z * sin;
            float wz = v.x * sin + v.z * cos;

            worldCorners[i] = new Vector3f(
                    (float) (entityPos.x + wx),
                    (float) (entityPos.y + v.y),
                    (float) (entityPos.z + wz)
            );
        }
        return worldCorners;
    }

    /** resolveWorldCornersの結果を包含する最小のAABB(当たり判定の実体として使う軽量版) */
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
}
