package com.mimic.monstermod.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 光るエフェクトを頂点から組み立てるための共通処理。
 *
 * ビームも竜巻も「加算合成で光る面をたくさん並べる」という点では同じなので、
 * 面を1枚流し込む処理をここにまとめている。
 *
 * 使う描画設定は {@code RenderType.eyes(texture)}:
 *   ・加算合成 → 黒い部分は何も描かれない(光っているように見える)
 *   ・明るさ計算を無視 → 夜でも洞窟でも同じように光る
 *   ・深度バッファに書き込まない → 層を重ねても互いに欠けない
 * ただし裏面が捨てられる設定なので、面は {@link #quadBothSides} で表裏1回ずつ描く。
 *
 * 呼び出し元: {@link BeamRenderer}, {@link VortexRenderer}
 */
public final class VfxRenderUtil {

    private VfxRenderUtil() {}

    /** 常に最大の明るさ。頂点フォーマットが要求するので値だけ渡す(シェーダは使わない) */
    public static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    /**
     * 四角を1枚。頂点の順番によっては裏面と判定されて消える。
     *
     * 【使い分けの注意】
     * 裏面を捨てる描画設定({@code RenderType.eyes} など)でこちらを使うと、
     * 向きが合っていない面は1枚も残らず消える。
     * カメラの方を向く板は特に事故が起きやすいので、
     * そういう設定では {@link #quadBothSides} を使うこと。
     * 裏面も描く設定({@code entityTranslucent} / {@code entityNoOutline})なら
     * こちらで問題ない。
     */
    public static void quad(PoseStack.Pose pose, VertexConsumer vc,
                            float r, float g, float b, float a,
                            float x1, float y1, float z1, float u1, float v1,
                            float x2, float y2, float z2, float u2, float v2,
                            float x3, float y3, float z3, float u3, float v3,
                            float x4, float y4, float z4, float u4, float v4) {
        quadLit(pose, vc, r, g, b, a, FULL_BRIGHT,
                x1, y1, z1, u1, v1, x2, y2, z2, u2, v2,
                x3, y3, z3, u3, v3, x4, y4, z4, u4, v4);
    }

    /**
     * 明るさを指定して四角を1枚。
     * 加算合成の描画設定は明るさを見ないが、通常の半透明(entityNoOutline など)は
     * 周囲の明るさの影響を受けるので、その場合はエンティティ位置の明るさを渡す。
     */
    public static void quadLit(PoseStack.Pose pose, VertexConsumer vc,
                               float r, float g, float b, float a, int light,
                               float x1, float y1, float z1, float u1, float v1,
                               float x2, float y2, float z2, float u2, float v2,
                               float x3, float y3, float z3, float u3, float v3,
                               float x4, float y4, float z4, float u4, float v4) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(m, n, vc, r, g, b, a, light, x1, y1, z1, u1, v1);
        vertex(m, n, vc, r, g, b, a, light, x2, y2, z2, u2, v2);
        vertex(m, n, vc, r, g, b, a, light, x3, y3, z3, u3, v3);
        vertex(m, n, vc, r, g, b, a, light, x4, y4, z4, u4, v4);
    }

    /**
     * 四角を表裏1枚ずつ。
     * この描画設定は裏面を捨てるため、どの角度から見ても消えないようにこちらを使う。
     * 加算合成なので重なった部分がより明るくなり、結果的に厚みも出る。
     */
    public static void quadBothSides(PoseStack.Pose pose, VertexConsumer vc,
                                     float r, float g, float b, float a,
                                     float x1, float y1, float z1, float u1, float v1,
                                     float x2, float y2, float z2, float u2, float v2,
                                     float x3, float y3, float z3, float u3, float v3,
                                     float x4, float y4, float z4, float u4, float v4) {
        quad(pose, vc, r, g, b, a,
                x1, y1, z1, u1, v1, x2, y2, z2, u2, v2,
                x3, y3, z3, u3, v3, x4, y4, z4, u4, v4);
        quad(pose, vc, r, g, b, a,
                x4, y4, z4, u4, v4, x3, y3, z3, u3, v3,
                x2, y2, z2, u2, v2, x1, y1, z1, u1, v1);
    }

    /** 頂点フォーマット NEW_ENTITY(座標・色・UV・オーバーレイ・明るさ・法線)に合わせて1点流す */
    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc,
                               float r, float g, float b, float a, int light,
                               float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
