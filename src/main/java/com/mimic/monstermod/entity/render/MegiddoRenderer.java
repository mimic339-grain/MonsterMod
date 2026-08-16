package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.obj.MegiddoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * メギドの描画。土星のような「黒い球 + 傾いた円盤」。
 *
 * 【構成】
 *  黒い球   … 光を通さない不透明の球。通常の半透明で描いているので、
 *              背景より暗くなり「穴が空いている」ように見える。
 *              中に少しだけ輝きを散らしてあり、ためが進むほど数が増える。
 *  円盤     … 30度ほど傾けた輪。土星の環のようにザラついた薄い黄色。
 *  吸い込み … 円盤の外から球へ向かって黒い粒が流れ込む。
 *              ためが進むほど速くなり、最後は一気に吸い込まれる。
 *  はじける … 集まったものが逆向きに放出される。白い閃光と外へ伸びる筋。
 *
 * 【粒を実体で出さない理由】
 * 数百個の粒をエンティティやパーティクルで出すと重く、同期もばらつく。
 * ここでは「何番目の粒か」と時間から位置を毎フレーム計算しているので、
 * 数を増やしても負担が小さく、全員に同じ動きが見える。
 */
public class MegiddoRenderer extends EntityRenderer<MegiddoEntity> {

    private static final ResourceLocation RING_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/ring.png");
    private static final ResourceLocation GLOW_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");
    private static final ResourceLocation BEAM_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam.png");

    /** 円盤の傾き。土星の見え方に近い角度 */
    private static final float RING_TILT_DEG = 28.0F;

    /** 球の分割数 */
    private static final int SPHERE_LON = 28;
    private static final int SPHERE_LAT = 14;

    /** 円盤の分割数と、内側/外側の半径(球の半径に対する倍率) */
    private static final int RING_SEGMENTS = 64;
    private static final float RING_INNER = 1.5F;
    private static final float RING_OUTER = 3.2F;

    /** 吸い込まれる粒の数 */
    private static final int MOTES = 220;
    /** 球の中の輝きの最大数。ためが進むほどここまで増える */
    private static final int SPARKS = 90;
    /** はじけたときに伸びる筋の数 */
    private static final int BURST_RAYS = 48;

    // 粒と輝きのばらつき。毎フレーム同じ動きになるよう固定の種から作る
    private static final float[] MOTE_ANGLE = new float[MOTES];
    private static final float[] MOTE_HEIGHT = new float[MOTES];
    private static final float[] MOTE_PHASE = new float[MOTES];
    private static final float[] MOTE_SPEED = new float[MOTES];
    private static final float[] MOTE_SIZE = new float[MOTES];

    private static final float[] SPARK_YAW = new float[SPARKS];
    private static final float[] SPARK_PITCH = new float[SPARKS];
    private static final float[] SPARK_DEPTH = new float[SPARKS];
    private static final float[] SPARK_BLINK = new float[SPARKS];

    private static final float[] RAY_YAW = new float[BURST_RAYS];
    private static final float[] RAY_PITCH = new float[BURST_RAYS];
    private static final float[] RAY_LEN = new float[BURST_RAYS];

    static {
        Random rng = new Random(20260819L);
        for (int i = 0; i < MOTES; i++) {
            MOTE_ANGLE[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            MOTE_HEIGHT[i] = (rng.nextFloat() - 0.5F) * 0.5F; // 円盤の厚みぶん散らす
            MOTE_PHASE[i] = rng.nextFloat();
            MOTE_SPEED[i] = 0.6F + rng.nextFloat() * 0.8F;
            MOTE_SIZE[i] = 0.10F + rng.nextFloat() * 0.16F;
        }
        for (int i = 0; i < SPARKS; i++) {
            SPARK_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            SPARK_PITCH[i] = (rng.nextFloat() - 0.5F) * (float) Math.PI;
            SPARK_DEPTH[i] = 0.55F + rng.nextFloat() * 0.42F;
            SPARK_BLINK[i] = rng.nextFloat() * 10.0F;
        }
        for (int i = 0; i < BURST_RAYS; i++) {
            RAY_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            RAY_PITCH[i] = (rng.nextFloat() - 0.5F) * (float) Math.PI;
            RAY_LEN[i] = 0.5F + rng.nextFloat() * 0.9F;
        }
    }

    public MegiddoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MegiddoEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {

        float radius = entity.getSphereRadius();
        float charge = entity.getChargeProgress(partialTick);
        boolean banging = entity.isBanging(partialTick);
        float bang = entity.getBangProgress(partialTick);

        float time = (float) (entity.level().getGameTime() % 24000L) + partialTick;

        pose.pushPose();
        pose.translate(0.0, radius + 1.0, 0.0); // 地面に埋まらないよう少し浮かせる

        if (!banging) {
            drawCharging(pose, buffer, radius, charge, time);
        } else {
            drawBigBang(pose, buffer, entity, radius, bang);
        }

        pose.popPose();
    }

    // ---------------- ためている間 ----------------

    private void drawCharging(PoseStack pose, MultiBufferSource buffer,
                              float radius, float charge, float time) {

        // 1. 黒い球。半透明で描くので背景より暗くなり、穴のように見える
        VertexConsumer solid = buffer.getBuffer(RenderType.entityTranslucent(GLOW_TEX));
        drawBlackSphere(pose, solid, radius);

        // 2. 円盤。傾けて土星のようにする
        VertexConsumer ring = buffer.getBuffer(RenderType.entityTranslucent(RING_TEX));
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(RING_TILT_DEG));
        pose.mulPose(Axis.YP.rotation(time * 0.006F)); // ゆっくり回す
        drawRing(pose.last(), ring, radius);
        pose.popPose();

        // 3. 吸い込まれる黒い粒。ためが進むほど速くなる
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(RING_TILT_DEG));
        drawMotes(pose, solid, radius, charge, time);
        pose.popPose();

        // 4. 球の中の輝き。ためが進むほど数が増える
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW_TEX));
        drawSparks(pose, glow, radius, charge, time);
    }

    /**
     * 光を通さない黒い球。
     * 通常の半透明で不透明に描くことで、背景より暗い「穴」に見せている
     * (加算合成では明るくしかできないので、この見え方は作れない)。
     */
    private static void drawBlackSphere(PoseStack pose, VertexConsumer vc, float radius) {
        PoseStack.Pose last = pose.last();

        for (int lat = 0; lat < SPHERE_LAT; lat++) {
            float t0 = (float) lat / SPHERE_LAT, t1 = (float) (lat + 1) / SPHERE_LAT;
            float p0 = (float) (Math.PI * (t0 - 0.5)), p1 = (float) (Math.PI * (t1 - 0.5));
            float y0 = Mth.sin(p0) * radius, r0 = Mth.cos(p0) * radius;
            float y1 = Mth.sin(p1) * radius, r1 = Mth.cos(p1) * radius;

            for (int lon = 0; lon < SPHERE_LON; lon++) {
                float u0 = (float) lon / SPHERE_LON, u1 = (float) (lon + 1) / SPHERE_LON;
                float a0 = (float) (Math.PI * 2.0 * u0), a1 = (float) (Math.PI * 2.0 * u1);

                float x00 = Mth.cos(a0) * r0, z00 = Mth.sin(a0) * r0;
                float x10 = Mth.cos(a1) * r0, z10 = Mth.sin(a1) * r0;
                float x01 = Mth.cos(a0) * r1, z01 = Mth.sin(a0) * r1;
                float x11 = Mth.cos(a1) * r1, z11 = Mth.sin(a1) * r1;

                // 真っ黒。テクスチャの中心(不透明な部分)だけを使う
                VfxRenderUtil.quadLit(last, vc, 0.02F, 0.02F, 0.03F, 1.0F,
                        VfxRenderUtil.FULL_BRIGHT,
                        x00, y0, z00, 0.5F, 0.5F,
                        x10, y0, z10, 0.5F, 0.5F,
                        x11, y1, z11, 0.5F, 0.5F,
                        x01, y1, z01, 0.5F, 0.5F);
            }
        }
    }

    /** 土星の環のような円盤。ザラつきと帯はテクスチャ側が持っている */
    private static void drawRing(PoseStack.Pose pose, VertexConsumer vc, float radius) {
        float inner = radius * RING_INNER;
        float outer = radius * RING_OUTER;

        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / RING_SEGMENTS);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / RING_SEGMENTS);
            float u0 = (float) i / RING_SEGMENTS, u1 = (float) (i + 1) / RING_SEGMENTS;

            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            // 薄い黄色。無機質に見せたいので彩度は低め
            float r = 0.95F, g = 0.90F, b = 0.72F;

            // 表と裏。円盤は薄いので、両側から見えないと消えたように見える
            VfxRenderUtil.quadLit(pose, vc, r, g, b, 0.85F, VfxRenderUtil.FULL_BRIGHT,
                    c0 * inner, 0, s0 * inner, u0, 0.0F,
                    c1 * inner, 0, s1 * inner, u1, 0.0F,
                    c1 * outer, 0, s1 * outer, u1, 1.0F,
                    c0 * outer, 0, s0 * outer, u0, 1.0F);
            VfxRenderUtil.quadLit(pose, vc, r, g, b, 0.85F, VfxRenderUtil.FULL_BRIGHT,
                    c0 * outer, 0, s0 * outer, u0, 1.0F,
                    c1 * outer, 0, s1 * outer, u1, 1.0F,
                    c1 * inner, 0, s1 * inner, u1, 0.0F,
                    c0 * inner, 0, s0 * inner, u0, 0.0F);
        }
    }

    /**
     * 円盤の外から球へ吸い込まれる黒い粒。
     *
     * 粒ごとの進み具合を「時間 × 速さ + 位相」の小数部で出しているので、
     * 端まで行った粒は自動的にまた外側から現れる。無限に湧いているように見える。
     * ためが進むほど掛ける速さを上げて、吸い込みが加速するようにしている。
     */
    private void drawMotes(PoseStack pose, VertexConsumer vc,
                           float radius, float charge, float time) {
        // 終盤ほど速く。最後は一気に吸い込まれる
        float speedScale = 0.35F + charge * charge * 2.4F;

        for (int i = 0; i < MOTES; i++) {
            float t = (MOTE_PHASE[i] + time * 0.004F * MOTE_SPEED[i] * speedScale) % 1.0F;
            if (t < 0) t += 1.0F;

            // t=0 で円盤の外、t=1 で球の表面
            float dist = Mth.lerp(t, radius * RING_OUTER, radius * 0.95F);
            float ang = MOTE_ANGLE[i] + t * 1.6F; // 巻き込まれながら落ちる
            float y = MOTE_HEIGHT[i] * radius * (1.0F - t); // 球に近づくほど平らになる

            float x = Mth.cos(ang) * dist;
            float z = Mth.sin(ang) * dist;

            // 球に飲まれる直前で消す。ぶつかって止まったように見えるのを避ける
            float alpha = Mth.clamp((1.0F - t) * 3.0F, 0.0F, 1.0F) * 0.9F;
            float size = MOTE_SIZE[i] * radius * 0.5F;

            drawBillboard(pose, vc, x, y, z, size, 0.02F, 0.02F, 0.04F, alpha);
        }
    }

    /**
     * 球の中の輝き。ためが進むほど数が増える。
     * 表面より少し内側に置いて、黒の中に沈んでいるように見せる。
     */
    private void drawSparks(PoseStack pose, VertexConsumer vc,
                            float radius, float charge, float time) {
        int count = Math.max(2, Mth.floor(SPARKS * charge * charge));

        for (int i = 0; i < count; i++) {
            float d = SPARK_DEPTH[i] * radius;
            float cy = Mth.cos(SPARK_PITCH[i]);
            float x = Mth.cos(SPARK_YAW[i]) * cy * d;
            float y = Mth.sin(SPARK_PITCH[i]) * d;
            float z = Mth.sin(SPARK_YAW[i]) * cy * d;

            // ちらつかせて「星のよう」に見せる
            float blink = 0.55F + 0.45F * Mth.sin(time * 0.25F + SPARK_BLINK[i]);
            float size = radius * 0.10F * blink;

            drawBillboard(pose, vc, x, y, z, size, 1.0F, 0.97F, 0.9F, blink);
        }
    }

    // ---------------- はじけた後 ----------------

    /**
     * ビッグバン。集まったものが逆向きに放出される。
     * 中心の閃光が膨らみながら薄れ、そこから四方八方へ筋が伸びていく。
     */
    private void drawBigBang(PoseStack pose, MultiBufferSource buffer,
                             MegiddoEntity entity, float radius, float bang) {

        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW_TEX));
        float reach = entity.getBlastRadius();

        // 中心の閃光。一気に膨らんで薄れる
        float flashSize = radius * (2.0F + bang * 14.0F);
        float flashAlpha = (1.0F - bang) * (1.0F - bang);
        drawBillboard(pose, glow, 0, 0, 0, flashSize, 1.0F, 0.98F, 0.92F, flashAlpha);

        // 放射状に飛び散る筋
        VertexConsumer streak = buffer.getBuffer(RenderType.eyes(BEAM_TEX));
        PoseStack.Pose last = pose.last();

        for (int i = 0; i < BURST_RAYS; i++) {
            float cy = Mth.cos(RAY_PITCH[i]);
            float dx = Mth.cos(RAY_YAW[i]) * cy;
            float dy = Mth.sin(RAY_PITCH[i]);
            float dz = Mth.sin(RAY_YAW[i]) * cy;

            // 先端は外へ、根元は少し遅れて追いかける
            float head = reach * RAY_LEN[i] * bang;
            float tail = Math.max(0.0F, head - reach * 0.35F);
            float w = radius * 0.22F * (1.0F - bang * 0.6F);

            // 筋の太さ方向。飛ぶ向きと直交していれば見た目上は十分
            float px = -dz * w, pz = dx * w;
            float alpha = (1.0F - bang) * 0.9F;

            VfxRenderUtil.quadBothSides(last, streak, 1.0F, 0.9F, 0.75F, alpha,
                    dx * tail + px, dy * tail, dz * tail + pz, 0.0F, 0.0F,
                    dx * tail - px, dy * tail, dz * tail - pz, 1.0F, 0.0F,
                    dx * head - px, dy * head, dz * head - pz, 1.0F, 1.0F,
                    dx * head + px, dy * head, dz * head + pz, 0.0F, 1.0F);
        }
    }

    // ---------------- 補助 ----------------

    /** カメラの方を向く小さな板。粒と輝きに使う */
    private void drawBillboard(PoseStack pose, VertexConsumer vc,
                               float x, float y, float z, float size,
                               float r, float g, float b, float a) {
        if (a <= 0.01F) return;

        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float h = size * 0.5F;
        VfxRenderUtil.quadLit(pose.last(), vc, r, g, b, a, VfxRenderUtil.FULL_BRIGHT,
                -h, -h, 0, 0, 0,
                 h, -h, 0, 1, 0,
                 h,  h, 0, 1, 1,
                -h,  h, 0, 0, 1);

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MegiddoEntity entity) {
        return RING_TEX;
    }
}
