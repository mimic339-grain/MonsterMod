package com.mimic.monstermod.client.renderer;

import com.mimic.monstermod.entity.base.CustomEntityBase;
import com.mimic.monstermod.model.anim.SkeletonPose;
import com.mimic.monstermod.model.parser.ParsedModel;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * GeckoLibやマイクラ標準描画を一切使わず、
 * BlenderポリゴンメッシュをJavaで直接描画するカスタムレンダラー。
 *
 * 描画フロー:
 *   1. SkeletonPose.update() でスキニング行列を更新
 *   2. 各頂点をスキニング行列で変換（CPU側スキニング）
 *   3. VertexConsumer に三角形単位でデータを流し込む
 *
 * 配置: com/mimic/monstermod/client/renderer/PolygonMeshRenderer.java
 */
public class PolygonMeshRenderer<T extends CustomEntityBase>
        extends EntityRenderer<T> {

    private final ParsedModel model;
    private final ResourceLocation texture;
    private final SkeletonPose pose;

    public PolygonMeshRenderer(EntityRendererProvider.Context ctx,
                               ParsedModel model,
                               ResourceLocation texture) {
        super(ctx);
        this.model   = model;
        this.texture = texture;
        this.pose    = new SkeletonPose(model);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        double animTime = entity.getAnimationTick() / 20.0;
        pose.update(model.animation, animTime);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
        poseStack.pushPose();
        poseStack.scale(1f, 1f, -1f);

        Matrix4f poseMat = poseStack.last().pose();
        Matrix4f[] skinMats = pose.getSkinningMatrices();

        for (int i = 0; i < model.indexBuffer.length; i += 3) {
            for (int j = 0; j < 3; j++) {
                int vi = model.indexBuffer[i + j];

                // 高速化したウェイトアクセス
                int[] boneIdx = model.skinnedBoneIndices[vi];
                float[] boneWt = model.skinnedWeights[vi];

                // 1. 位置のスキニングとワールド変換
                Vector3f bindPos = new Vector3f(model.positions[vi*3], model.positions[vi*3+1], model.positions[vi*3+2]);
                Vector3f skinnedPos = pose.skinVertex(bindPos, boneIdx, boneWt);
                Vector3f worldPos = poseMat.transformPosition(skinnedPos, new Vector3f());

                // 2. 法線のスキニング（逆転置行列を考慮した変換）
                Vector3f bindNorm = new Vector3f(model.normals[vi*3], model.normals[vi*3+1], model.normals[vi*3+2]);
                Vector3f skinnedNorm = skinNormal(bindNorm, boneIdx, boneWt, skinMats);

                consumer.vertex(worldPos.x, worldPos.y, worldPos.z)
                        .color(255, 255, 255, 255)
                        .uv(model.uvs[vi*2], model.uvs[vi*2+1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(skinnedNorm.x, skinnedNorm.y, skinnedNorm.z)
                        .endVertex();
            }
        }
        poseStack.popPose();
    }

    /** 法線のスキニング計算 */
    private Vector3f skinNormal(Vector3f normal, int[] indices, float[] weights, Matrix4f[] mats) {
        Vector3f result = new Vector3f(0, 0, 0);
        for (int i = 0; i < indices.length; i++) {
            if (weights[i] <= 0) continue;
            // 法線は回転行列のみを適用（平行移動成分を含まない）
            Matrix4f m = mats[indices[i]];
            Vector3f transformed = m.transformDirection(new Vector3f(normal));
            result.add(transformed.mul(weights[i]));
        }
        return result.normalize(); // 正規化して返す
    }

    // ── ウェイトデータを頂点インデックスから取り出すヘルパー ──────
    private int[] getBoneIndices(int vertexIndex) {
        int[] vcounts = model.vcounts;
        int[] vindices = model.vindices;

        int offset = 0;
        for (int i = 0; i < vertexIndex; i++) offset += vcounts[i] * 2;

        int count = vcounts[vertexIndex];
        int[] result = new int[count];
        for (int j = 0; j < count; j++) result[j] = vindices[offset + j * 2];
        return result;
    }

    private float[] getBoneWeights(int vertexIndex) {
        int[] vcounts = model.vcounts;
        int[] vindices = model.vindices;
        float[] weights = model.weights;

        int offset = 0;
        for (int i = 0; i < vertexIndex; i++) offset += vcounts[i] * 2;

        int count = vcounts[vertexIndex];
        float[] result = new float[count];
        for (int j = 0; j < count; j++) result[j] = weights[vindices[offset + j * 2 + 1]];
        return result;
    }
}