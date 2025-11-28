package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AttackPreview3DMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * AoeRenderer3D
 *
 * 3D攻撃予兆（AoE）マーカーをワールド上に描画するクラス
 * AttackPreview3DMath で計算した ShapeData（頂点・面・エッジ情報）を使って描画
 *
 * - faces: ポリゴン面を描画
 * - edges: エッジ（線）を描画
 * - ライティング: ワールドの明るさに応じて頂点のライト値を計算
 */
public class AoeRenderer3D {

    /** 攻撃予兆のテクスチャ */
    private static final ResourceLocation TEX =
            new ResourceLocation("monstermod", "textures/misc/attackpreview.png");

    // ============================================================================
    //  MAIN RENDER METHOD
    // ============================================================================
    /**
     * ShapeData を元にワールド上に 3D AoE を描画
     *
     * @param poseStack 描画用の座標行列スタック
     * @param buffers   描画バッファ
     * @param shape     描画する 3D形状データ（頂点・面・エッジ情報）
     * @param partialTicks 描画フレーム補間値
     */
    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            AttackPreview3DMath.ShapeData shape,
            float partialTicks
    ) {
        if (shape == null) return;

        // ========================
        //  FACES (面) の描画
        // ========================
        if (shape.surfaces != null) {
            VertexConsumer faceVC = buffers.getBuffer(RenderType.entityTranslucentCull(TEX));
            poseStack.pushPose();  // 座標行列を保存
            for (AttackPreview3DMath.Quad q : shape.surfaces) {
                drawQuad(poseStack, faceVC, q);
            }
            poseStack.popPose();  // 座標行列を復元
        }
    }

    // ============================================================================
    //  DRAW QUAD (1つの四角形を描画)
    // ============================================================================
    private static void drawQuad(PoseStack stack, VertexConsumer vc, AttackPreview3DMath.Quad quad) {
        PoseStack.Pose pose = stack.last();  // 現在の座標変換行列

        // normal.y が負の場合は頂点順序を反転して裏面描画を防止
        int[] order = quad.normal.y < 0 ? new int[]{0,3,2,1} : new int[]{0,1,2,3};

        for (int i : order) {
            Vec3 p = quad.pos[i];       // 頂点座標
            float u = quad.uv[i * 2];   // テクスチャ U
            float v = quad.uv[i * 2 + 1]; // テクスチャ V
            putVertex(vc, pose, p, u, v, quad.rgba, quad.normal);
        }
    }

    // ============================================================================
    //  DRAW SINGLE VERTEX
    // ============================================================================
    private static void putVertex(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 pos,
            float u, float v,
            float[] rgba,
            Vec3 normal
    ) {
        int light = getPackedLight(pos); // ワールドライティング値取得

        vc.vertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]) // RGBA
                .uv(u, v)                                 // テクスチャ
                .overlayCoords(OverlayTexture.NO_OVERLAY) // オーバーレイなし
                .uv2(light)                               // 光源値
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z) // 法線
                .endVertex();                             // 頂点描画終了
    }
    // ============================================================================
    //  LIGHT CALCULATION
    // ============================================================================
    /**
     * 指定位置のワールドライト値を取得
     *
     * @param pos ワールド座標
     * @return packedLight（ライト・スカイライトをパックした値）
     */
    private static int getPackedLight(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 240; // デフォルト明るさ

        BlockPos bp = new BlockPos(
                (int) Math.floor(pos.x),
                (int) Math.floor(pos.y),
                (int) Math.floor(pos.z)
        );
        // ワールドのブロック光源と太陽光を取得
        int sky = mc.level.getBrightness(LightLayer.SKY, bp);
        int block = mc.level.getBrightness(LightLayer.BLOCK, bp);

        return LightTexture.pack(block, sky); // 1つの整数にパック
    }
}
