package com.mimic.monstermod.collision;

import com.mimic.monstermod.entity.base.CustomEntityBase;
import com.mimic.monstermod.model.anim.SkeletonPose;
import com.mimic.monstermod.model.parser.EntityModelData.ColliderData;
import com.mimic.monstermod.model.parser.ParsedModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * スキニング行列からOBBを同期し、攻撃ヒット判定を処理するシステム。
 *
 * 処理フロー（毎tick）:
 *   1. SkeletonPoseからボーンのグローバル行列を取得
 *   2. 各OBBコライダーをそのボーン行列で更新（同期）
 *   3. プレイヤーの攻撃点とOBBを照合して部位ヒット判定
 *
 * 配置: com/mimic/monstermod/collision/OBBHitDetector.java
 */
public class OBBHitDetector {

    private final CustomEntityBase entity;
    private final ParsedModel model;
    private final Map<String, OBBCollider> colliderMap = new LinkedHashMap<>();

    public OBBHitDetector(CustomEntityBase entity, ParsedModel model) {
        this.entity = entity;
        this.model  = model;

        // コライダーを初期化
        if (model.colliders != null) {
            for (ColliderData cd : model.colliders) {
                OBBCollider obb = new OBBCollider(
                        new Vector3f(cd.center[0], cd.center[1], cd.center[2]),
                        new Vector3f(cd.halfExtents[0], cd.halfExtents[1], cd.halfExtents[2]),
                        new Quaternionf(cd.orientation[1], cd.orientation[2],
                                cd.orientation[3], cd.orientation[0])
                );
                colliderMap.put(cd.name, obb);
            }
        }
    }

    /**
     * スキニング行列からOBBを毎tickで更新（ボーン→OBB同期）。
     *
     * @param pose    現在のスキニングポーズ
     * @param entityPos エンティティのワールド位置
     */
    public void syncFromPose(SkeletonPose pose, Vec3 entityPos) {
        if (model.colliders == null) return;
        Matrix4f[] skinMats = pose.getSkinningMatrices();

        for (ColliderData cd : model.colliders) {
            Integer boneIdx = model.boneIndexMap.get(cd.boneName);
            if (boneIdx == null || boneIdx >= skinMats.length) continue;

            // ボーンのワールド行列にエンティティ位置を加算
            Matrix4f boneWorld = new Matrix4f(skinMats[boneIdx])
                    .translate((float)entityPos.x, (float)entityPos.y, (float)entityPos.z);

            OBBCollider obb = colliderMap.get(cd.name);
            if (obb != null) {
                obb.updateFromBoneMatrix(boneWorld,
                        new Vector3f(cd.halfExtents[0], cd.halfExtents[1], cd.halfExtents[2]));

                // エンティティのOBBマップも更新（クライアント/サーバー共有用）
                entity.getOBBMap().put(cd.boneName, new CustomEntityBase.OBBData(
                        obb.center, obb.halfExtents, obb.orientation, cd.partGroup));
            }
        }
    }

    /**
     * 攻撃ヒット判定：プレイヤーがエンティティを攻撃したとき、
     * どの部位（OBB）に当たったかを返す。
     *
     * @param attacker    攻撃したプレイヤー
     * @param attackRange 攻撃リーチ
     * @return ヒットした部位名（当たらなければnull）
     */
    public String detectHit(Player attacker, float attackRange) {
        Vec3 eyePos = attacker.getEyePosition();
        Vec3 lookVec = attacker.getLookAngle();

        Vector3f rayOrigin = new Vector3f((float)eyePos.x, (float)eyePos.y, (float)eyePos.z);
        Vector3f rayDir    = new Vector3f((float)lookVec.x, (float)lookVec.y, (float)lookVec.z)
                .normalize();

        String hitPart = null;
        float minDist = attackRange;

        for (Map.Entry<String, OBBCollider> entry : colliderMap.entrySet()) {
            float t = entry.getValue().raycast(rayOrigin, rayDir);
            if (t >= 0 && t < minDist) {
                minDist = t;
                // コライダー名からpartGroupを取得
                hitPart = getPartGroup(entry.getKey());
            }
        }

        return hitPart;
    }

    private String getPartGroup(String colliderName) {
        if (model.colliders == null) return colliderName;
        return model.colliders.stream()
                .filter(cd -> cd.name.equals(colliderName))
                .map(cd -> cd.partGroup)
                .findFirst()
                .orElse(colliderName);
    }

    public Map<String, OBBCollider> getColliderMap() { return colliderMap; }
}