package com.mimic.monstermod.client;

import com.mimic.monstermod.identity.bomber.BomberIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * ブロックに仕掛けたボム(踏むと起動するやつ)を、ボマー本人にだけ見せる目印。
 *
 * 【自分の仕掛けが見えないと成立しない】
 * 見た目が普通のブロックのままなので、仕掛けた側も
 * どこに置いたか忘れると自分で踏んでしまう。
 * かといって全員に見えると罠にならないので、
 * サーバーがボマーにだけ位置を送り({@link com.mimic.monstermod.network.server.S2C_BlockBombMarksPacket})、
 * ここで赤い箱を重ねて描いている。
 *
 * 設置ボムのほうは見た目からしてボムなので、この目印は付けない。
 */
@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlockBombMarks {

    private BlockBombMarks() {}

    /** 仕掛けてある場所。ボマーにしか届かないので、ボマーでなければ空のまま */
    private static final List<BlockPos> SECRET = new ArrayList<>();
    /** 踏まれて起動した場所。証拠として全員に届く */
    private static final List<BlockPos> REVEALED = new ArrayList<>();

    public static void replaceAll(List<BlockPos> secret, List<BlockPos> revealed) {
        SECRET.clear();
        SECRET.addAll(secret);
        REVEALED.clear();
        REVEALED.addAll(revealed);
    }

    public static void clear() {
        SECRET.clear();
        REVEALED.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (SECRET.isEmpty() && REVEALED.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());

        // 仕掛けてある場所。念のため描く側でもボマーかどうかを確認する
        if (BomberIdentity.of(mc.player) != null) {
            for (BlockPos pos : SECRET) drawAt(pose, vc, pos, cam, 1.0F, 0.15F, 0.15F);
        }

        // 踏まれた場所。誰にでも見える証拠なので、少し明るい色にして区別する
        for (BlockPos pos : REVEALED) drawAt(pose, vc, pos, cam, 1.0F, 0.55F, 0.10F);

        buffer.endBatch(RenderType.lines());
    }

    private static void drawAt(PoseStack pose, VertexConsumer vc, BlockPos pos, Vec3 cam,
                               float r, float g, float b) {
        // 遠すぎるものは描かない(線が画面を埋めるため)
        if (pos.distToCenterSqr(cam.x, cam.y, cam.z) > 64.0 * 64.0) return;

        pose.pushPose();
        pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        drawCage(pose.last(), vc, r, g, b);
        pose.popPose();
    }

    /** ブロックを囲う枠 */
    private static void drawCage(PoseStack.Pose pose, VertexConsumer vc, float r, float g, float b) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        float lo = -0.01F, hi = 1.01F; // ブロックより気持ち大きく囲う
        float[][] c = {
                {lo,lo,lo}, {hi,lo,lo}, {hi,lo,hi}, {lo,lo,hi},
                {lo,hi,lo}, {hi,hi,lo}, {hi,hi,hi}, {lo,hi,hi}
        };
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };
        for (int[] e : edges) {
            float[] p1 = c[e[0]], p2 = c[e[1]];
            line(m, n, vc, p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], r, g, b);
        }
    }

    private static void line(Matrix4f m, Matrix3f n, VertexConsumer vc,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.0F) return;
        dx /= len; dy /= len; dz /= len;

        vc.vertex(m, x1, y1, z1).color(r, g, b, 0.9F).normal(n, dx, dy, dz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 0.9F).normal(n, dx, dy, dz).endVertex();
    }
}
