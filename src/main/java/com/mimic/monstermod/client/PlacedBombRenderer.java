package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.block.PlacedBombBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 設置ボムの爆発範囲を、面を貼った球で見せる。
 *
 * 【線ではなく面にした理由】
 * 線だけだと細く、どこまでが範囲なのか掴みにくい。
 * 既存のスキルプレビュー(AoeRenderer3D)と同じく、
 * 半透明の面を貼った球にして「この中は巻き込まれる」と一目で分かるようにしている。
 *
 * 【いつ・誰に見えるか】
 * 時間が決まった(armed)瞬間から、その場にいる全員に見える。
 * 危険信号なので、仕掛けた本人だけに見えても意味がない。
 * 置いただけで時間未設定のうちは出さない(まだ危険ではないため)。
 *
 * 中身(残り時間・半径)は BlockEntity としてクライアントへ同期されているので、
 * ここでは受け取った値を描くだけでよい。
 */
public class PlacedBombRenderer implements BlockEntityRenderer<PlacedBombBlockEntity> {

    /** スキルプレビューと同じ絵を使い、見た目の作法を揃える */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/misc/attackpreview.png");

    /** 球の分割数。横(経度)と縦(緯度) */
    private static final int LON = 32;
    private static final int LAT = 16;

    /** 面の濃さ。濃すぎると中の様子が見えなくなる */
    private static final float ALPHA = 0.22F;
    /** 裏面を少し浮かせる量。同じ位置に重ねるとちらつくため */
    private static final float OFFSET = 0.02F;

    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    public PlacedBombRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PlacedBombBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {

        // 時間が決まるまでは危険ではないので出さない
        if (!be.isArmed() || be.getRadius() <= 0.1F) return;

        // 残り10秒を切ったら点滅させて、最終警告だと分かるようにする
        float alpha = ALPHA;
        if (be.getFuseTicks() < 200) {
            alpha = ((be.getFuseTicks() / 5) % 2 == 0) ? ALPHA * 1.8F : ALPHA * 0.5F;
        }

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5); // 球の中心をボムに合わせる

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        drawSphere(pose.last(), vc, be.getRadius(), alpha);

        pose.popPose();
    }

    /**
     * 緯度・経度で分割した球を、四角形の面で貼る。
     * 表と裏の両方を出しているのは、中に入ったときにも壁が見えるようにするため。
     */
    private static void drawSphere(PoseStack.Pose pose, VertexConsumer vc, float radius, float alpha) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        for (int lat = 0; lat < LAT; lat++) {
            float t0 = (float) lat / LAT;
            float t1 = (float) (lat + 1) / LAT;
            // 緯度は -90度 〜 +90度
            float p0 = (float) (Math.PI * (t0 - 0.5));
            float p1 = (float) (Math.PI * (t1 - 0.5));

            float y0 = Mth.sin(p0) * radius, r0 = Mth.cos(p0) * radius;
            float y1 = Mth.sin(p1) * radius, r1 = Mth.cos(p1) * radius;

            for (int lon = 0; lon < LON; lon++) {
                float u0 = (float) lon / LON;
                float u1 = (float) (lon + 1) / LON;
                float a0 = (float) (Math.PI * 2.0 * u0);
                float a1 = (float) (Math.PI * 2.0 * u1);

                float x00 = Mth.cos(a0) * r0, z00 = Mth.sin(a0) * r0;
                float x10 = Mth.cos(a1) * r0, z10 = Mth.sin(a1) * r0;
                float x01 = Mth.cos(a0) * r1, z01 = Mth.sin(a0) * r1;
                float x11 = Mth.cos(a1) * r1, z11 = Mth.sin(a1) * r1;

                // 表
                quad(m, n, vc, alpha,
                        x00, y0, z00, u0, t0,
                        x10, y0, z10, u1, t0,
                        x11, y1, z11, u1, t1,
                        x01, y1, z01, u0, t1);

                // 裏(内側から見たとき用)。わずかに内へずらしてちらつきを避ける
                float s = 1.0F - OFFSET / Math.max(1.0F, radius);
                quad(m, n, vc, alpha,
                        x01 * s, y1 * s, z01 * s, u0, t1,
                        x11 * s, y1 * s, z11 * s, u1, t1,
                        x10 * s, y0 * s, z10 * s, u1, t0,
                        x00 * s, y0 * s, z00 * s, u0, t0);
            }
        }
    }

    private static void quad(Matrix4f m, Matrix3f n, VertexConsumer vc, float alpha,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float x4, float y4, float z4, float u4, float v4) {
        vertex(m, n, vc, alpha, x1, y1, z1, u1, v1);
        vertex(m, n, vc, alpha, x2, y2, z2, u2, v2);
        vertex(m, n, vc, alpha, x3, y3, z3, u3, v3);
        vertex(m, n, vc, alpha, x4, y4, z4, u4, v4);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer vc, float alpha,
                               float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
                .color(1.0F, 0.15F, 0.12F, alpha)   // 危険を示す赤
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    /** ブロックが画面外でも球が消えないようにする */
    @Override
    public boolean shouldRenderOffScreen(PlacedBombBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }
}
