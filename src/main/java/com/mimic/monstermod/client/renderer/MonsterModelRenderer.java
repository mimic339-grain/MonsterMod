package com.mimic.monstermod.client.renderer;

import com.mimic.monstermod.client.model.MonsterArmatureCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * GeckoLibを使わないカスタムモデルレンダラー。
 *
 * EFM参考:
 *   - api/client/EpicFightRenderer.java
 *   - api/client/ArmatureRenderer.java
 *
 * 描画フロー:
 *   1. MonsterArmatureCacheからArmatureデータを取得（Blender JSONから読み込み済み）
 *   2. 現在のアニメーション状態からSkeletonPoseを計算（Lerp/Slerp補間）
 *   3. VertexConsumerでポリゴンを直接書き込む
 *
 * GeckoLib除去チェックリスト:
 *   ☑ build.gradle から geckolib-forge dependency を削除
 *   ☑ geo/ パッケージを全削除
 *   ☑ このクラスで独自レンダリング
 *   ☐ Blender JSON → Armature パイプラインの完成 (Phase 2)
 *
 * 配置: com/mimic/monstermod/client/renderer/MonsterModelRenderer.java
 */
@OnlyIn(Dist.CLIENT)
public class MonsterModelRenderer {

    /**
     * Monster変身中プレイヤーをBlenderモデルで描画する。
     *
     * @param player       描画対象プレイヤー
     * @param monsterType  モンスター種別 (e.g., "mimic", "dragon")
     * @param headYaw      頭のyaw（サーバー同期値を使うこと）
     * @param headPitch    頭のpitch
     * @param partialTick  補間係数
     */
    public static void render(Player player, String monsterType,
                              float headYaw, float headPitch, float partialTick,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        // ── プレイヤーArmatureインスタンスを取得 ─────────────────────
        // EFM: EpicFightRenderer が EntityPatch から Animator を取得するパターン
        MonsterArmatureCache.MonsterArmature armature =
                MonsterArmatureCache.getOrCreate(player.getUUID(), monsterType);
        if (!armature.isReady()) return; // モデルが未ロードならスキップ

        // ── LivingMotionManagerでモーション状態を更新 ─────────────────
        // EFM: Animator.updateMotion() パターン
        armature.motionManager.updateMotion(player);

        // ── AnimationPlayerで補間済み時刻を計算 ──────────────────────
        // EFM: AnimationPlayer.getCurrentPose(partialTicks) パターン
        com.mimic.monstermod.model.parser.ParsedModel model = armature.model;
        com.mimic.monstermod.model.anim.AnimationPlayer animPlayer = armature.animPlayer;

        // アニメーションデータ取得
        com.mimic.monstermod.model.parser.ParsedModel.ParsedAnimation anim = model.animation;
        float totalTime = (anim != null && anim.tracks != null && !anim.tracks.isEmpty())
                ? (float) anim.tracks.values().iterator().next().times[
                anim.tracks.values().iterator().next().times.length - 1]
                : 1.0f;
        animPlayer.tick(totalTime);
        float animTime = animPlayer.getInterpolatedTime(partialTick);

        // ── SkeletonPoseでスキニング行列を計算 ────────────────────────
        // EFM: AnimationPlayer.getCurrentPose() → Pose → skinningMatrices
        com.mimic.monstermod.model.anim.SkeletonPose skeletonPose =
                new com.mimic.monstermod.model.anim.SkeletonPose(model);
        skeletonPose.update(anim, animTime);
        org.joml.Matrix4f[] skinMats = skeletonPose.getSkinningMatrices();

        // ── テクスチャ取得 ────────────────────────────────────────────
        ResourceLocation texture = new ResourceLocation(
                "monstermod", "textures/entity/" + monsterType + ".png");

        // ── PoseStack設定 ─────────────────────────────────────────────
        poseStack.pushPose();

        // プレイヤーの位置・回転をモデルに適用（サーバー同期値を使う）
        poseStack.mulPose(new org.joml.Quaternionf()
                .rotationY((float) Math.toRadians(-headYaw)));

        // Blender→Minecraft座標系変換
        poseStack.scale(1f, 1f, -1f);

        // ── VertexConsumerで頂点を直接書き込み ────────────────────────
        // EFM: ArmatureRenderer.renderVertices() パターン (Phase 3: PolygonMeshRenderer)
        var consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(texture));
        org.joml.Matrix4f mat = poseStack.last().pose();

        int[] indices = model.indexBuffer;
        float[] pos   = model.positions;
        float[] uvArr = model.uvs;
        float[] norms = model.normals;

        if (indices != null && pos != null) {
            for (int i = 0; i < indices.length; i += 3) {
                for (int j = 0; j < 3; j++) {
                    int vi = indices[i + j];
                    org.joml.Vector3f bindPos = new org.joml.Vector3f(
                            pos[vi * 3], pos[vi * 3 + 1], pos[vi * 3 + 2]);
                    // CPU側スキニング（SkeletonPose.skinVertex）
                    int[] boneIdx = getBoneIndices(model, vi);
                    float[] boneWt = getBoneWeights(model, vi);
                    org.joml.Vector3f skinnedPos = skeletonPose.skinVertex(bindPos, boneIdx, boneWt);
                    org.joml.Vector3f worldPos = mat.transformPosition(skinnedPos, new org.joml.Vector3f());
                    consumer.vertex(worldPos.x, worldPos.y, worldPos.z)
                            .color(255, 255, 255, 255)
                            .uv(uvArr != null ? uvArr[vi * 2] : 0, uvArr != null ? uvArr[vi * 2 + 1] : 0)
                            .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                            .uv2(packedLight)
                            .normal(norms != null ? norms[vi*3] : 0, norms != null ? norms[vi*3+1] : 1, norms != null ? norms[vi*3+2] : 0)
                            .endVertex();
                }
            }
        }

        poseStack.popPose();
    }

    // ── 頂点ウェイト取得ヘルパー ──────────────────────────────────────
    private static int[] getBoneIndices(com.mimic.monstermod.model.parser.ParsedModel model, int vi) {
        if (model.vcounts == null || model.vindices == null) return new int[0];
        int offset = 0;
        for (int i = 0; i < vi; i++) offset += model.vcounts[i] * 2;
        int count = model.vcounts[vi];
        int[] r = new int[count];
        for (int j = 0; j < count; j++) r[j] = model.vindices[offset + j * 2];
        return r;
    }

    private static float[] getBoneWeights(com.mimic.monstermod.model.parser.ParsedModel model, int vi) {
        if (model.vcounts == null || model.vindices == null || model.weights == null) return new float[0];
        int offset = 0;
        for (int i = 0; i < vi; i++) offset += model.vcounts[i] * 2;
        int count = model.vcounts[vi];
        float[] r = new float[count];
        for (int j = 0; j < count; j++) r[j] = model.weights[model.vindices[offset + j * 2 + 1]];
        return r;
    }
}