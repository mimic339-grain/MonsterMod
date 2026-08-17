package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.render.VfxRenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * 血の入った石を持っている間、足元の前方に相手の方角を指す矢印を出す。
 *
 * 【HUDではなくワールドに描く理由】
 * 画面に貼り付けた矢印だと、地形との位置関係が読めない。
 * 地面に置いて向きを指させると「その方向へ走ればよい」が直感的に分かる。
 * 矢印は常に自分の少し前の地面に置き、向きだけを相手のほうへ回している。
 *
 * 【表示条件は「新しい座標が届いているか」だけ】
 * サーバーは「血の入った石を持っている人」にしか座標を送らないので、
 * 届いているという事実だけで「持っている」ことが確定する。
 * 石をしまえば送信が止まり、一定時間で自然に消える。消すためのパケットは要らない。
 *
 * 以前はここでも手持ちのアイテムのNBTを見て判定していたが、
 * クライアント側のNBTが届いていないと矢印ごと出なくなるため、
 * 表示に必要な情報(名前・座標)はすべてパケットに載せてもらう形に変えた。
 * 描画はサーバーが送ってくる情報だけで完結する。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public final class BloodStoneCompass {

    private BloodStoneCompass() {}

    /** 頂点の色をそのまま出すための真っ白な板 */
    private static final ResourceLocation WHITE =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/white.png");

    /** 矢印を置く位置(自分から前方何ブロックか) */
    private static final double AHEAD = 2.2;
    /** 地面とのちらつきを避けるための浮かせ量 */
    private static final double LIFT = 0.06;
    /** この時間ぶん座標が来なければ表示をやめる(tick) */
    private static final int STALE_TICKS = 20;
    /** 高さの差がこれ未満なら「同じ高さ」とみなす(ブロック) */
    private static final double FLAT_RANGE = 2.0;

    private static final String[] DIRS = { "南", "南東", "東", "北東", "北", "北西", "西", "南西" };

    // --- サーバーから届いた最新の情報。描画に必要なものはここに全部そろう ---
    private static String targetName = "";
    private static boolean online;
    private static boolean sameDimension;
    private static double tx, ty, tz;
    private static long receivedAt = Long.MIN_VALUE;

    /**
     * 座標を受け取る。
     * 呼び出し元: {@link com.mimic.monstermod.network.server.S2C_BloodStoneTargetPacket#handle}
     */
    public static void receive(String name, boolean isOnline, boolean isSameDimension,
                               double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        targetName = name;
        online = isOnline;
        sameDimension = isSameDimension;
        tx = x; ty = y; tz = z;
        receivedAt = mc.level == null ? Long.MIN_VALUE : mc.level.getGameTime();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui) return;

        // 石をしまうとサーバーが送るのをやめるので、情報が古ければ描かない。
        // 逆に言えば、新しい情報が来ている＝血の入った石を持っている、で確定する
        if (mc.level.getGameTime() - receivedAt > STALE_TICKS) return;

        float partial = event.getPartialTick();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 self = new Vec3(
                Mth.lerp(partial, player.xo, player.getX()),
                Mth.lerp(partial, player.yo, player.getY()),
                Mth.lerp(partial, player.zo, player.getZ()));

        // 自分が向いている方向の少し前、足元の高さに置く
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getViewYRot(partial));
        Vec3 at = self.add(facing.scale(AHEAD)).add(0.0, LIFT, 0.0);

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        if (!online) {
            drawLabel(pose, buffer, mc, at, cam, targetName + " は今いない", 0xFF8888);
            return;
        }
        if (!sameDimension) {
            drawLabel(pose, buffer, mc, at, cam, targetName + " は別の世界にいる", 0xFFAA55);
            return;
        }

        double dx = tx - self.x;
        double dz = tz - self.z;
        double dy = ty - self.y;
        double flat = Math.sqrt(dx * dx + dz * dz);

        // 真上・真下にいるときは向きが定まらないので、矢印は自分の向きのままにする
        Vec3 toTarget = flat < 0.01 ? facing : new Vec3(dx / flat, 0.0, dz / flat);

        drawArrow(pose, buffer, at, cam, toTarget);
        drawLabel(pose, buffer, mc, at, cam, buildLabel(flat, dy, dx, dz), 0xFFFFFF);
    }

    /** 「38m 北東 ↑12m」のような1行を作る */
    private static String buildLabel(double flat, double dy, double dx, double dz) {
        StringBuilder sb = new StringBuilder();
        sb.append(Math.round(flat)).append("m  ");

        // 方角。+Z が南、+X が東
        double deg = Math.toDegrees(Math.atan2(dx, dz));
        if (deg < 0) deg += 360.0;
        sb.append(DIRS[((int) Math.round(deg / 45.0)) & 7]);

        if (Math.abs(dy) < FLAT_RANGE) {
            sb.append("  同じ高さ");
        } else if (dy > 0) {
            sb.append("  ↑").append(Math.round(dy)).append("m");
        } else {
            sb.append("  ↓").append(Math.round(-dy)).append("m");
        }
        return sb.toString();
    }

    /**
     * 地面に寝かせた矢印を1つ描く。
     *
     * ヨー角に直さず、相手への向き(forward)とそれに直交する向き(right)を作って
     * 頂点を組み立てている。こうするとヨーの基準がどちら回りかを気にしなくてよい。
     *
     * 縁取りを先に、本体をあとに描く。
     * この描画設定は深度に書き込まないので、同じ高さに置いても重なり順は描いた順のままになる。
     */
    private static void drawArrow(PoseStack pose, MultiBufferSource.BufferSource buffer,
                                  Vec3 at, Vec3 cam, Vec3 forward) {
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);

        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        PoseStack.Pose last = pose.last();

        VertexConsumer vc = buffer.getBuffer(RenderType.entityNoOutline(WHITE));

        // 縁取り(黒)。本体より一回り大きく描く
        arrowShape(last, vc, forward, right, 1.22F, 0.06F, 0.01F, 0.02F, 0.85F);
        // 本体(血の赤)
        arrowShape(last, vc, forward, right, 1.00F, 0.86F, 0.10F, 0.12F, 1.00F);

        buffer.endBatch(RenderType.entityNoOutline(WHITE));
        pose.popPose();
    }

    /** 矢印の形。軸(f)と横(r)の長さで指定し、胴と鏃の2枚で作る */
    private static void arrowShape(PoseStack.Pose pose, VertexConsumer vc,
                                   Vec3 f, Vec3 r, float scale,
                                   float cr, float cg, float cb, float ca) {
        float shaftBack = -0.55F * scale, shaftFront = 0.25F * scale;
        float shaftHalf = 0.17F * scale;
        float headHalf = 0.45F * scale;
        float tip = 1.00F * scale;

        // 胴(長方形)
        quadOnGround(pose, vc, f, r, shaftBack, -shaftHalf, shaftFront, shaftHalf, cr, cg, cb, ca);

        // 鏃(先端を1点につぶした四角＝三角形)
        float[] a = ground(f, r, shaftFront, -headHalf);
        float[] b = ground(f, r, shaftFront,  headHalf);
        float[] c = ground(f, r, tip, 0.0F);
        VfxRenderUtil.quadLit(pose, vc, cr, cg, cb, ca, VfxRenderUtil.FULL_BRIGHT,
                a[0], 0, a[1], 0.0F, 0.0F,
                b[0], 0, b[1], 1.0F, 0.0F,
                c[0], 0, c[1], 1.0F, 1.0F,
                c[0], 0, c[1], 0.0F, 1.0F);
    }

    /** 地面に寝かせた長方形を1枚 */
    private static void quadOnGround(PoseStack.Pose pose, VertexConsumer vc, Vec3 f, Vec3 r,
                                     float f0, float r0, float f1, float r1,
                                     float cr, float cg, float cb, float ca) {
        float[] p00 = ground(f, r, f0, r0);
        float[] p10 = ground(f, r, f0, r1);
        float[] p11 = ground(f, r, f1, r1);
        float[] p01 = ground(f, r, f1, r0);

        VfxRenderUtil.quadLit(pose, vc, cr, cg, cb, ca, VfxRenderUtil.FULL_BRIGHT,
                p00[0], 0, p00[1], 0.0F, 0.0F,
                p10[0], 0, p10[1], 1.0F, 0.0F,
                p11[0], 0, p11[1], 1.0F, 1.0F,
                p01[0], 0, p01[1], 0.0F, 1.0F);
    }

    /** 前方fw・右rwぶん進んだ点のXZ座標 */
    private static float[] ground(Vec3 f, Vec3 r, float fw, float rw) {
        return new float[] {
                (float) (f.x * fw + r.x * rw),
                (float) (f.z * fw + r.z * rw)
        };
    }

    /** 矢印の上に、距離と方角と高さの差を出す */
    private static void drawLabel(PoseStack pose, MultiBufferSource.BufferSource buffer,
                                  Minecraft mc, Vec3 at, Vec3 cam, String text, int color) {
        Font font = mc.font;

        pose.pushPose();
        pose.translate(at.x - cam.x, at.y + 0.85 - cam.y, at.z - cam.z);
        // 常にカメラの方を向かせる。文字は上下が逆になるのでスケールで反転する
        pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f m = pose.last().pose();
        float x = -font.width(text) / 2.0F;
        font.drawInBatch(text, x, 0.0F, color, false, m, buffer,
                Font.DisplayMode.NORMAL, 0, VfxRenderUtil.FULL_BRIGHT);
        buffer.endBatch();

        pose.popPose();
    }
}
