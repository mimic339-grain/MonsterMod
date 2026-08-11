package com.mimic.monstermod.entity.hitbox;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 部位パーツの位置を、現在のアニメーションに合わせて更新する共通処理。
 * 実体のモンスターと変身プレイヤーの両方から呼ばれる。
 *
 * 【重要】クライアント・サーバーの両方で毎tick呼ぶこと。
 * 攻撃対象の選択(レイピック)はクライアントが行うため、クライアント側の
 * パーツ位置が更新されていないと照準が部位に当たらずダメージが入らない。
 */
public final class BoneHitboxUpdater {

    private BoneHitboxUpdater() {}

    public static void update(BoneRigData rig, BoneHitboxPart[] parts,
                             String animation, double elapsedSeconds,
                             Vec3 ownerPos, float ownerYaw) {
        if (animation == null || animation.isEmpty()) return;

        for (BoneHitboxPart part : parts) {
            if (!part.isEnabled() || part.getConfig() == null) continue;

            Vector3f[] corners = BonePoseResolver.resolveWorldCorners(
                    rig, part.getConfig().boneName(), animation, elapsedSeconds, ownerPos, ownerYaw);
            if (corners == null) continue;
            part.updateFromWorldAABB(BonePoseResolver.enclosingAABB(corners));
        }
    }
}
