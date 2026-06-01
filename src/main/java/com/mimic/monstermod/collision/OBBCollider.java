package com.mimic.monstermod.collision;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * OBB（有向境界ボックス）衝突判定クラス。
 *
 * 【分離軸定理（SAT）による実装】
 *   2つのOBBが衝突していないなら、それらを分離する平面（分離軸）が必ず存在する。
 *   - 各OBBの3軸（計6軸）
 *   - 各軸のクロス積（計9軸）
 *   合計15軸すべてで重なりがあれば衝突。1つでも分離できれば非衝突。
 *
 * 配置: com/mimic/monstermod/collision/OBBCollider.java
 */
public class OBBCollider {

    public final Vector3f center;
    public final Vector3f halfExtents;
    public final Quaternionf orientation;

    /** ローカル軸（ワールド空間）: right, up, forward */
    private final Vector3f[] axes = new Vector3f[3];

    public OBBCollider(Vector3f center, Vector3f halfExtents, Quaternionf orientation) {
        this.center = new Vector3f(center);
        this.halfExtents = new Vector3f(halfExtents);
        this.orientation = new Quaternionf(orientation);
        updateAxes();
    }

    /** ボーン行列からOBBを更新する */
    public void updateFromBoneMatrix(Matrix4f boneMatrix, Vector3f halfExtents) {
        boneMatrix.getTranslation(this.center);
        boneMatrix.getUnnormalizedRotation(this.orientation);
        this.halfExtents.set(halfExtents);
        updateAxes();
    }

    private void updateAxes() {
        // 回転行列からローカル軸を抽出
        axes[0] = orientation.transform(new Vector3f(1, 0, 0)); // Right
        axes[1] = orientation.transform(new Vector3f(0, 1, 0)); // Up
        axes[2] = orientation.transform(new Vector3f(0, 0, 1)); // Forward
    }

    // ── 点との衝突判定 ───────────────────────────────────────────
    /**
     * 点がOBBの内部にあるかを判定する（攻撃ヒット判定に使用）。
     */
    public boolean containsPoint(Vector3f point) {
        Vector3f local = new Quaternionf(orientation).conjugate()
                .transform(new Vector3f(point).sub(center));
        return Math.abs(local.x) <= halfExtents.x
                && Math.abs(local.y) <= halfExtents.y
                && Math.abs(local.z) <= halfExtents.z;
    }

    // ── OBBとOBBの衝突判定（SAT） ────────────────────────────────
    /**
     * 2つのOBBが交差しているかをSAT（分離軸定理）で判定する。
     * Phase 3の主要実装：15軸すべてをチェック。
     *
     * @param other 相手のOBB
     * @return 交差していればtrue
     */
    public boolean intersects(OBBCollider other) {
        // 分離軸リスト（最大15軸）
        Vector3f[] testAxes = new Vector3f[15];

        // 自分の3軸
        testAxes[0] = axes[0];
        testAxes[1] = axes[1];
        testAxes[2] = axes[2];

        // 相手の3軸
        testAxes[3] = other.axes[0];
        testAxes[4] = other.axes[1];
        testAxes[5] = other.axes[2];

        // クロス積による9軸
        int idx = 6;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector3f cross = new Vector3f(axes[i]).cross(other.axes[j]);
                // ゼロベクトルをスキップ（平行な場合）
                if (cross.lengthSquared() > 1e-6f) {
                    testAxes[idx++] = cross.normalize();
                }
            }
        }

        Vector3f diff = new Vector3f(other.center).sub(center);

        for (int i = 0; i < idx; i++) {
            Vector3f axis = testAxes[i];
            if (axis == null) continue;

            float projA = projectOBB(this, axis);
            float projB = projectOBB(other, axis);
            float dist  = Math.abs(diff.dot(axis));

            // 分離軸が見つかれば非衝突
            if (dist > projA + projB) return false;
        }

        // 全軸で重なり → 衝突
        return true;
    }

    /**
     * OBBをある軸に射影したときの半幅を計算する。
     */
    private static float projectOBB(OBBCollider obb, Vector3f axis) {
        return Math.abs(obb.axes[0].dot(axis)) * obb.halfExtents.x
                + Math.abs(obb.axes[1].dot(axis)) * obb.halfExtents.y
                + Math.abs(obb.axes[2].dot(axis)) * obb.halfExtents.z;
    }

    // ── レイキャスト ─────────────────────────────────────────────
    /**
     * レイとOBBの交差判定（弓矢・プロジェクタイル用）。
     *
     * @param rayOrigin レイの始点
     * @param rayDir    レイの方向（正規化済み）
     * @return 交差距離（tmin）、交差しない場合は -1
     */
    public float raycast(Vector3f rayOrigin, Vector3f rayDir) {
        // OBBのローカル空間にレイを変換
        Quaternionf invRot = new Quaternionf(orientation).conjugate();
        Vector3f localOrigin = invRot.transform(new Vector3f(rayOrigin).sub(center));
        Vector3f localDir    = invRot.transform(new Vector3f(rayDir));

        float tmin = Float.NEGATIVE_INFINITY;
        float tmax = Float.POSITIVE_INFINITY;

        float[] he = { halfExtents.x, halfExtents.y, halfExtents.z };
        float[] lo = { localOrigin.x, localOrigin.y, localOrigin.z };
        float[] ld = { localDir.x,    localDir.y,    localDir.z    };

        for (int i = 0; i < 3; i++) {
            if (Math.abs(ld[i]) < 1e-6f) {
                if (Math.abs(lo[i]) > he[i]) return -1f;
            } else {
                float t1 = (-he[i] - lo[i]) / ld[i];
                float t2 = ( he[i] - lo[i]) / ld[i];
                if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return -1f;
            }
        }
        return tmin >= 0 ? tmin : -1f;
    }
}