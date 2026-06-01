package com.mimic.monstermod.client;


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
    public void render(T entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight) {

        // ① スキニング行列を現在のアニメーション時刻で更新
        double animTime = entity.getAnimationTick() / 20.0; // tickをsecに変換
        pose.update(model.animation, animTime);

        // ② 描画バッファを取得
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutout(texture));

        poseStack.pushPose();
        // Blender→Minecraft座標系変換（Y=上）
        poseStack.scale(1f, 1f, -1f);

        Matrix4f transform = poseStack.last().pose();
        Matrix4f[] skinMats = pose.getSkinningMatrices();

        // ③ 三角形インデックスバッファを走査して描画
        int[] indices = model.indexBuffer;
        float[] pos   = model.positions;
        float[] uvs   = model.uvs;
        float[] norms = model.normals;

        for (int i = 0; i < indices.length; i += 3) {
            for (int j = 0; j < 3; j++) {
                int vi = indices[i + j]; // 頂点インデックス

                // バインドポーズの位置
                Vector3f bindPos = new Vector3f(
                        pos[vi * 3],
                        pos[vi * 3 + 1],
                        pos[vi * 3 + 2]
                );

                // スキニング
                int[] boneIdx = getBoneIndices(vi);
                float[] boneWt = getBoneWeights(vi);
                Vector3f skinnedPos = pose.skinVertex(bindPos, boneIdx, boneWt);

                // ワールド変換
                Vector3f worldPos = transform.transformPosition(skinnedPos, new Vector3f());

                // 法線（簡易: スキニングせず元の法線を使用）
                // Phase 3改善: 法線もスキニング行列の逆転置で変換する
                int ni = vi; // 法線インデックス（今回は頂点と同じ）

                consumer.vertex(worldPos.x, worldPos.y, worldPos.z)
                        .color(255, 255, 255, 255)
                        .uv(uvs[vi * 2], uvs[vi * 2 + 1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(norms[ni*3], norms[ni*3+1], norms[ni*3+2])
                        .endVertex();
            }
        }

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
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