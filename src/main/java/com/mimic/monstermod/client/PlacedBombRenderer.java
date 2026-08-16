package com.mimic.monstermod.client;

import com.mimic.monstermod.block.PlacedBombBlockEntity;
import com.mimic.monstermod.identity.bomber.BomberIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 設置ボムの見せ方。
 *
 * 【誰から見ても球が出る理由】
 * 半径のプレビューは「危険信号」なので、持っている本人だけに見えても意味がない。
 * BlockEntity の中身はクライアントにも同期されているため、
 * 時間が決まった(armed)瞬間から、その場にいる全員に球が見え続ける。
 *
 * 【ボマーだけ真っ赤に見える理由】
 * 仕掛けた側は自分の仕掛けを一目で把握したい。
 * ブロックのテクスチャ自体を差し替えると全員に見えてしまうので、
 * ボマーのときだけ赤い箱を上から重ねている。
 *
 * 球は面ではなく線で描く。面だと中が見えず、どこまで巻き込まれるか分からないため。
 */
public class PlacedBombRenderer implements BlockEntityRenderer<PlacedBombBlockEntity> {

    /** 輪1本の分割数。少ないと多角形に見える */
    private static final int SEGMENTS = 48;

    public PlacedBombRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PlacedBombBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5); // ブロックの中心へ

        boolean bomber = isViewerBomber();

        // 仕掛けた側だけ、ブロックが真っ赤に見えるようにする
        if (bomber) {
            drawRedCage(pose, buffer.getBuffer(RenderType.lines()));
        }

        // 時間が決まっているなら、爆発半径を全員に見せる
        if (be.isArmed() && be.getRadius() > 0.1F) {
            drawSphereOutline(pose, buffer.getBuffer(RenderType.lines()), be.getRadius(), be);
        }

        pose.popPose();
    }

    /** 見ている本人がボマーか。ボマーのときだけ仕掛けが目立つようにする */
    private static boolean isViewerBomber() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && BomberIdentity.of(mc.player) != null;
    }

    /** ブロックを囲う赤い枠。ボマー本人にだけ見える目印 */
    private static void drawRedCage(PoseStack pose, VertexConsumer vc) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float h = 0.52F; // ブロックより気持ち大きく囲う

        float[][] corners = {
                {-h,-h,-h}, { h,-h,-h}, { h,-h, h}, {-h,-h, h},
                {-h, h,-h}, { h, h,-h}, { h, h, h}, {-h, h, h}
        };
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},   // 下面
                {4,5},{5,6},{6,7},{7,4},   // 上面
                {0,4},{1,5},{2,6},{3,7}    // 柱
        };
        for (int[] e : edges) {
            float[] a = corners[e[0]], b = corners[e[1]];
            line(m, n, vc, a[0], a[1], a[2], b[0], b[1], b[2], 1.0F, 0.1F, 0.1F, 1.0F);
        }
    }

    /**
     * 直交する3つの輪で球の輪郭を描く。
     * 残りが少なくなるほど赤みを強くして、切迫具合が見た目でも分かるようにする。
     */
    private static void drawSphereOutline(PoseStack pose, VertexConsumer vc, float radius,
                                          PlacedBombBlockEntity be) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();

        // 残り10秒を切ったら点滅させる(最終警告)
        float alpha = 0.75F;
        if (be.getFuseTicks() < 200) {
            boolean on = (be.getFuseTicks() / 5) % 2 == 0;
            alpha = on ? 1.0F : 0.25F;
        }

        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / SEGMENTS);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / SEGMENTS);

            float c0 = Mth.cos(a0) * radius, s0 = Mth.sin(a0) * radius;
            float c1 = Mth.cos(a1) * radius, s1 = Mth.sin(a1) * radius;

            // 横(XZ) … 地面に沿った広がりが一番知りたいので主役
            line(m, n, vc, c0, 0, s0, c1, 0, s1, 1.0F, 0.15F, 0.15F, alpha);
            // 縦2枚で球に見せる
            line(m, n, vc, c0, s0, 0, c1, s1, 0, 1.0F, 0.15F, 0.15F, alpha * 0.7F);
            line(m, n, vc, 0, c0, s0, 0, c1, s1, 1.0F, 0.15F, 0.15F, alpha * 0.7F);
        }
    }

    private static void line(Matrix4f m, Matrix3f n, VertexConsumer vc,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        // 線の描画は向きも要求されるので、線分の方向を渡す
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.0F) return;
        dx /= len; dy /= len; dz /= len;

        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(n, dx, dy, dz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).normal(n, dx, dy, dz).endVertex();
    }

    /** ブロックが画面外でも球が消えないよう、遠くても描画対象にする */
    @Override
    public boolean shouldRenderOffScreen(PlacedBombBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
