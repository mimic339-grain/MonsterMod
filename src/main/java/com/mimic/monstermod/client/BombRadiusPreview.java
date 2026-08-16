package com.mimic.monstermod.client;

import com.mimic.monstermod.init.ModBlocks;
import com.mimic.monstermod.item.PlacedBombItem;
import com.mimic.monstermod.bomb.BombTiming;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 設置ボムの爆発半径を赤い線で見せるプレビュー。
 *
 * 【いつ出るか】
 *  ・設置ボムを手に持っているとき … これから置く場所(見ている先)に、選んだ時間ぶんの半径
 *  ・設置済みのボムを見ているとき … そのボムの実際の半径
 *
 * 【球を線で描く理由】
 * 面で描くと中が見えなくなり、どこまで巻き込まれるのか分からない。
 * 直交する3つの輪だけを線で引くと、中を覗きながら広がりが掴める。
 * 線は深度を無視して描いているので、地形の裏に回っても輪郭が追える。
 */
@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BombRadiusPreview {

    private BombRadiusPreview() {}

    /** 輪1本の分割数。少ないと多角形に見える */
    private static final int SEGMENTS = 48;
    /** 手に持っているときに、どこまで先を見て置き場所を判定するか */
    private static final double REACH = 6.0;

    private static final float R = 1.0F, G = 0.15F, B = 0.15F, A = 0.85F;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 半透明より後ろの段階で描く。地形に隠れず線が最後まで見える
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Target target = findTarget(mc);
        if (target == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        pose.pushPose();
        pose.translate(target.center.x - cam.x, target.center.y - cam.y, target.center.z - cam.z);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        // 深度を無視する線。地形の向こう側に回っても半径が把握できる
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());
        drawSphereOutline(pose, vc, target.radius);
        buffer.endBatch(RenderType.lines());

        pose.popPose();
    }

    /** 今プレビューを出すべき対象(中心と半径)を決める */
    private static Target findTarget(Minecraft mc) {
        // 1. 設置ボムを手に持っている → 置こうとしている場所を見せる
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof PlacedBombItem) {
            var hit = mc.player.pick(REACH, mc.getFrameTime(), false);
            if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr
                    && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());
                int fuse = PlacedBombItem.selectedSeconds(held) * BombTiming.TICKS_PER_SECOND;
                return new Target(Vec3.atCenterOf(pos), BombTiming.radiusForFuse(fuse));
            }
            return null;
        }

        // 2. 設置済みのボムを見ている → そのボムの半径を見せる
        var hit = mc.hitResult;
        if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            if (mc.level.getBlockState(pos).is(ModBlocks.PLACED_BOMB.get())) {
                // 半径はサーバー側が持っているので、ここでは見た目の目安として
                // 一番長い設定(2分)の半径を出す。実際の値はブロックを右クリックすると分かる
                int fuse = PlacedBombItem.FUSE_SECONDS[PlacedBombItem.FUSE_SECONDS.length - 1]
                        * BombTiming.TICKS_PER_SECOND;
                return new Target(Vec3.atCenterOf(pos), BombTiming.radiusForFuse(fuse));
            }
        }
        return null;
    }

    /** 直交する3つの輪で球の輪郭を描く */
    private static void drawSphereOutline(PoseStack pose, VertexConsumer vc, float radius) {
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();

        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / SEGMENTS);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / SEGMENTS);

            float c0 = Mth.cos(a0) * radius, s0 = Mth.sin(a0) * radius;
            float c1 = Mth.cos(a1) * radius, s1 = Mth.sin(a1) * radius;

            // 横(XZ) … 地面に沿った広がりが一番知りたいので、これが主役
            line(m, normal, vc, c0, 0, s0, c1, 0, s1);
            // 縦(XY)
            line(m, normal, vc, c0, s0, 0, c1, s1, 0);
            // 縦(YZ)
            line(m, normal, vc, 0, c0, s0, 0, c1, s1);
        }
    }

    private static void line(Matrix4f m, org.joml.Matrix3f n, VertexConsumer vc,
                             float x1, float y1, float z1, float x2, float y2, float z2) {
        // 線の描画は「向き」も要求されるので、線分の方向を渡す
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.0F) return;
        dx /= len; dy /= len; dz /= len;

        vc.vertex(m, x1, y1, z1).color(R, G, B, A).normal(n, dx, dy, dz).endVertex();
        vc.vertex(m, x2, y2, z2).color(R, G, B, A).normal(n, dx, dy, dz).endVertex();
    }

    /** プレビューの中心と半径 */
    private record Target(Vec3 center, float radius) {}
}
