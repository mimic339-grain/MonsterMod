package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AttackPreview3DMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class AoeRenderer3D {
    // 攻撃予兆用のテクスチャ
    private static final ResourceLocation TEX =
            new ResourceLocation("monstermod", "textures/misc/attackpreview.png");
    //描画のメイン処理
    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            AttackPreview3DMath.ShapeData shape,
            float partialTicks
    ) {
        if (shape == null) return;

        // Faces（面）: 両面描画
        if (shape.surfaces != null) {
            VertexConsumer faceVC = buffers.getBuffer(RenderType.entityTranslucent(TEX));
            poseStack.pushPose();  // 現在の座標・回転行列を保存
            for (AttackPreview3DMath.Quad q : shape.surfaces) {
                drawQuadDoubleSided(poseStack, faceVC, q);  // 両面描画
            }
            poseStack.popPose();  // 行列を復元
        }

        // Edges（枠線）
        if (shape.edges != null) {
            drawEdges(poseStack, buffers, shape);
        }
    }


    // 面を両面描画
    private static void drawQuadDoubleSided(PoseStack stack, VertexConsumer vc, AttackPreview3DMath.Quad quad) {
        PoseStack.Pose pose = stack.last();

        // 表面
        for (int i = 0; i < 4; i++) {
            Vec3 p = quad.pos[i];
            float u = quad.uv[i * 2];
            float v = quad.uv[i * 2 + 1];
            putVertex(vc, pose, p, u, v, quad.rgba, quad.normal);
        }

        // 裏面：頂点順を逆にし、法線を反転 + 0.01fだけ法線方向にオフセット
        for (int i = 3; i >= 0; i--) {
            Vec3 p = quad.pos[i].add(quad.normal.scale(0.0001)); // 少し前方にオフセット
            float u = quad.uv[i * 2];
            float v = quad.uv[i * 2 + 1];
            Vec3 flippedNormal = quad.normal.scale(-1); // 法線反転
            putVertex(vc, pose, p, u, v, quad.rgba, flippedNormal);
        }
    }

    // 頂点をバッファに書き込む
    private static void putVertex(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 pos,
            float u, float v,
            float[] rgba,
            Vec3 normal
    ) {
        vc.vertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z) // 座標
                .color(rgba[0], rgba[1], rgba[2], rgba[3]) // 面の色（RGBA）
                .uv(u, v) // テクスチャUV
                .overlayCoords(OverlayTexture.NO_OVERLAY) // オーバーレイなし
                .uv2(240) // ライティング情報
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z) // 法線
                .endVertex();
    }

    // 枠線描画
    private static void drawEdges(
            PoseStack stack,
            MultiBufferSource buffers,
            AttackPreview3DMath.ShapeData shape
    ) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lines()); // 線描画用
        PoseStack.Pose pose = stack.last();

        for (AttackPreview3DMath.Edge e : shape.edges) {
            Vec3 a = e.a; // エッジの始点
            Vec3 b = e.b; // エッジの終点
            int light = 240; // 明るさ取得

            // 枠線の始点を描画
            vc.vertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z)
                    .color(1f, 0f, 0f, 0.7f) // 赤色に変更 + 透明度0.7
                    .uv2(light)
                    .normal(pose.normal(), 0, 1, 0) // Y方向の法線（線描画用）
                    .endVertex();

            // 枠線の終点を描画
            vc.vertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z)
                    .color(1f, 0f, 0f, 0.7f) // 赤色
                    .uv2(light)
                    .normal(pose.normal(), 0, 1, 0)
                    .endVertex();
        }
    }

}
