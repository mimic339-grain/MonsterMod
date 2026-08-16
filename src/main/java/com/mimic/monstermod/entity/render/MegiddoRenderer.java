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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

/**
 * メギドの描画。土星のような「黒い球 + 傾いた円盤」。
 *
 * 【構成】
 *  黒い球   … 光を通さない不透明の球。通常の半透明で描くので背景より暗くなり、
 *              穴が空いているように見える。
 *  星       … 球の表面すれすれに散らした細かい光。エンドの星空のような見え方。
 *              ためが進むほど数が増えていく。
 *  円盤     … 地面に近い角度で傾けた輪。土星の環のようにザラついた薄い黄色。
 *  黒い粒   … 円盤の面に沿って外から球へ流れ込む砂粒。
 *  光の粒   … 全方向(360度)から球へ集まってくる光。
 *              最初は無く、ためが進むにつれて数が増え、動きも加速していく。
 *  はじける … 集めた光が一気に外へばらまかれ、減速しながら粉雪のように落ちて消える。
 *
 * 【粒を実体で出さない理由】
 * 千個単位の粒をエンティティやパーティクルで出すと重く、同期もばらつく。
 * ここでは「何番目の粒か」と時間から位置を毎フレーム計算しているので、
 * 数を増やしても負担が小さく、全員に同じ動きが見える。
 */
public class MegiddoRenderer extends EntityRenderer<MegiddoEntity> {

    private static final ResourceLocation RING_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/ring.png");
    private static final ResourceLocation GLOW_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");

    /** 円盤の傾き。土星のように、地面に近い浅い角度にする */
    private static final float RING_TILT_DEG = 14.0F;

    /** 球の分割数 */
    private static final int SPHERE_LON = 30;
    private static final int SPHERE_LAT = 16;

    /** 円盤の分割数と、内側/外側の半径(球の半径に対する倍率) */
    private static final int RING_SEGMENTS = 72;
    private static final float RING_INNER = 1.6F;
    private static final float RING_OUTER = 3.4F;

    /** 円盤の面に沿って流れ込む黒い粒 */
    private static final int DUST = 200;
    /** 全方向から集まってくる光の粒 */
    private static final int LIGHTS = 420;
    /** 球の表面に散らす星 */
    private static final int STARS = 300;
    /** はじけたときにばらまく光の粒 */
    private static final int BANG_MOTES = 1500;

    // --- 黒い粒(円盤沿い) ---
    private static final float[] DUST_ANGLE = new float[DUST];
    private static final float[] DUST_HEIGHT = new float[DUST];
    private static final float[] DUST_PHASE = new float[DUST];
    private static final float[] DUST_SPEED = new float[DUST];
    private static final float[] DUST_SIZE = new float[DUST];

    // --- 光の粒(全方向) ---
    private static final float[] LIGHT_YAW = new float[LIGHTS];
    private static final float[] LIGHT_PITCH = new float[LIGHTS];
    private static final float[] LIGHT_PHASE = new float[LIGHTS];
    private static final float[] LIGHT_SPEED = new float[LIGHTS];
    private static final float[] LIGHT_START = new float[LIGHTS];
    private static final float[] LIGHT_SIZE = new float[LIGHTS];

    // --- 表面の星 ---
    private static final float[] STAR_YAW = new float[STARS];
    private static final float[] STAR_PITCH = new float[STARS];
    private static final float[] STAR_BLINK = new float[STARS];
    private static final float[] STAR_SIZE = new float[STARS];

    // --- はじけたときの粒 ---
    private static final float[] BANG_YAW = new float[BANG_MOTES];
    private static final float[] BANG_PITCH = new float[BANG_MOTES];
    private static final float[] BANG_DIST = new float[BANG_MOTES];
    private static final float[] BANG_SIZE = new float[BANG_MOTES];
    private static final float[] BANG_FALL = new float[BANG_MOTES];

    static {
        Random rng = new Random(20260819L);

        for (int i = 0; i < DUST; i++) {
            DUST_ANGLE[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            DUST_HEIGHT[i] = (rng.nextFloat() - 0.5F) * 0.35F;
            DUST_PHASE[i] = rng.nextFloat();
            DUST_SPEED[i] = 0.6F + rng.nextFloat() * 0.8F;
            DUST_SIZE[i] = 0.08F + rng.nextFloat() * 0.14F;
        }
        for (int i = 0; i < LIGHTS; i++) {
            LIGHT_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            // 上下も含めた全方向に散らす。偏らないよう緯度は正弦で分布させる
            LIGHT_PITCH[i] = (float) Math.asin(rng.nextFloat() * 2.0F - 1.0F);
            LIGHT_PHASE[i] = rng.nextFloat();
            LIGHT_SPEED[i] = 0.7F + rng.nextFloat() * 0.7F;
            LIGHT_START[i] = 3.6F + rng.nextFloat() * 3.4F; // 球の半径に対する倍率
            LIGHT_SIZE[i] = 0.06F + rng.nextFloat() * 0.10F;
        }
        for (int i = 0; i < STARS; i++) {
            STAR_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            STAR_PITCH[i] = (float) Math.asin(rng.nextFloat() * 2.0F - 1.0F);
            STAR_BLINK[i] = rng.nextFloat() * 20.0F;
            STAR_SIZE[i] = 0.020F + rng.nextFloat() * 0.045F;
        }
        for (int i = 0; i < BANG_MOTES; i++) {
            BANG_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            BANG_PITCH[i] = (float) Math.asin(rng.nextFloat() * 2.0F - 1.0F);
            // 到達距離をばらけさせる。揃えると境界に丸い線が見えてしまう
            BANG_DIST[i] = 0.25F + rng.nextFloat() * rng.nextFloat() * 1.35F;
            BANG_SIZE[i] = 0.05F + rng.nextFloat() * 0.13F;
            BANG_FALL[i] = 0.5F + rng.nextFloat() * 1.2F;
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

        // カメラの向きは1回だけ求めて使い回す。粒ごとに行列を積むと数が増えたとき重い
        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        Vector3f right = new Vector3f(1, 0, 0).rotate(cam);
        Vector3f up = new Vector3f(0, 1, 0).rotate(cam);

        pose.pushPose();
        pose.translate(0.0, entity.getCenterOffset(), 0.0);

        if (!banging) {
            drawCharging(pose, buffer, radius, charge, time, right, up);
        } else {
            drawBigBang(pose, buffer, entity, radius, bang, time, right, up);
        }

        pose.popPose();
    }

    // ---------------- ためている間 ----------------

    private void drawCharging(PoseStack pose, MultiBufferSource buffer, float radius,
                              float charge, float time, Vector3f right, Vector3f up) {

        // 1. 黒い球。半透明で不透明に描くので、背景より暗い穴に見える
        VertexConsumer solid = buffer.getBuffer(RenderType.entityTranslucent(GLOW_TEX));
        drawBlackSphere(pose.last(), solid, radius);

        // 2. 円盤。
        // 深度を書かない設定にしているのは、書くと自分の面同士や球と干渉して
        // 輪が欠けて見えるため。球より後に描くので、球の裏側だけは正しく隠れる
        VertexConsumer ring = buffer.getBuffer(RenderType.entityNoOutline(RING_TEX));
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(RING_TILT_DEG));
        pose.mulPose(Axis.YP.rotation(time * 0.004F)); // ゆっくり回す
        drawRing(pose.last(), ring, radius);

        // 3. 円盤の面に沿って流れ込む黒い粒
        drawDust(pose.last(), solid, radius, charge, time, right, up);
        pose.popPose();

        // 4. 球の表面の星と、全方向から集まる光
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW_TEX));
        drawStars(pose.last(), glow, radius, charge, time, right, up);
        drawIncomingLights(pose.last(), glow, radius, charge, time, right, up);
    }

    /**
     * 光を通さない黒い球。
     * 通常の半透明で不透明に描くことで、背景より暗い「穴」に見せている
     * (加算合成では明るくしかできないので、この見え方は作れない)。
     */
    private static void drawBlackSphere(PoseStack.Pose pose, VertexConsumer vc, float radius) {
        for (int lat = 0; lat < SPHERE_LAT; lat++) {
            float t0 = (float) lat / SPHERE_LAT, t1 = (float) (lat + 1) / SPHERE_LAT;
            float p0 = (float) (Math.PI * (t0 - 0.5)), p1 = (float) (Math.PI * (t1 - 0.5));
            float y0 = Mth.sin(p0) * radius, r0 = Mth.cos(p0) * radius;
            float y1 = Mth.sin(p1) * radius, r1 = Mth.cos(p1) * radius;

            for (int lon = 0; lon < SPHERE_LON; lon++) {
                float a0 = (float) (Math.PI * 2.0 * lon / SPHERE_LON);
                float a1 = (float) (Math.PI * 2.0 * (lon + 1) / SPHERE_LON);

                float x00 = Mth.cos(a0) * r0, z00 = Mth.sin(a0) * r0;
                float x10 = Mth.cos(a1) * r0, z10 = Mth.sin(a1) * r0;
                float x01 = Mth.cos(a0) * r1, z01 = Mth.sin(a0) * r1;
                float x11 = Mth.cos(a1) * r1, z11 = Mth.sin(a1) * r1;

                // 真っ黒。テクスチャの中心(不透明な部分)だけを使う
                VfxRenderUtil.quadLit(pose, vc, 0.015F, 0.015F, 0.025F, 1.0F,
                        VfxRenderUtil.FULL_BRIGHT,
                        x00, y0, z00, 0.5F, 0.5F,
                        x10, y0, z10, 0.5F, 0.5F,
                        x11, y1, z11, 0.5F, 0.5F,
                        x01, y1, z01, 0.5F, 0.5F);
            }
        }
    }

    /** 土星の環のような円盤。帯とザラつきはテクスチャ側が持っている */
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
            VfxRenderUtil.quadLit(pose, vc, r, g, b, 0.9F, VfxRenderUtil.FULL_BRIGHT,
                    c0 * inner, 0, s0 * inner, u0, 0.0F,
                    c1 * inner, 0, s1 * inner, u1, 0.0F,
                    c1 * outer, 0, s1 * outer, u1, 1.0F,
                    c0 * outer, 0, s0 * outer, u0, 1.0F);
            VfxRenderUtil.quadLit(pose, vc, r, g, b, 0.9F, VfxRenderUtil.FULL_BRIGHT,
                    c0 * outer, 0, s0 * outer, u0, 1.0F,
                    c1 * outer, 0, s1 * outer, u1, 1.0F,
                    c1 * inner, 0, s1 * inner, u1, 0.0F,
                    c0 * inner, 0, s0 * inner, u0, 0.0F);
        }
    }

    /**
     * 円盤の面に沿って外から球へ流れ込む黒い粒。
     *
     * 粒ごとの進み具合を「時間 × 速さ + 位相」の小数部で出しているので、
     * 端まで行った粒は自動的にまた外側から現れる。無限に湧いているように見える。
     */
    private static void drawDust(PoseStack.Pose pose, VertexConsumer vc, float radius,
                                 float charge, float time, Vector3f right, Vector3f up) {
        // ためが進むほど速く。速すぎると砂嵐に見えるので控えめにしてある
        float speed = 0.18F + charge * charge * 1.0F;

        for (int i = 0; i < DUST; i++) {
            float t = frac(DUST_PHASE[i] + time * 0.004F * DUST_SPEED[i] * speed);

            float dist = Mth.lerp(t, radius * RING_OUTER, radius * 0.95F);
            float ang = DUST_ANGLE[i] + t * 1.6F;        // 巻き込まれながら落ちる
            float y = DUST_HEIGHT[i] * radius * (1.0F - t); // 球に近づくほど平らになる

            float alpha = Mth.clamp((1.0F - t) * 3.0F, 0.0F, 1.0F) * 0.9F;

            spark(pose, vc, Mth.cos(ang) * dist, y, Mth.sin(ang) * dist,
                    DUST_SIZE[i] * radius * 0.5F, right, up, 0.02F, 0.02F, 0.04F, alpha);
        }
    }

    /**
     * 球の表面に散らした星。
     *
     * 球のすぐ外側に置いているので、手前側だけが見え、裏側は球に隠れる。
     * これでエンドの星空のように「黒い球の中で瞬いている」ように見える。
     * ためが進むほど数が増え、吸い込んだものが溜まっていく感じになる。
     */
    private static void drawStars(PoseStack.Pose pose, VertexConsumer vc, float radius,
                                  float charge, float time, Vector3f right, Vector3f up) {
        // 最初から少しだけ瞬いていないと、ただの黒い玉に見えてしまう
        int count = Mth.clamp(Mth.floor(STARS * (0.08F + charge * charge * 0.92F)), 8, STARS);
        float d = radius * 1.006F;

        for (int i = 0; i < count; i++) {
            float cy = Mth.cos(STAR_PITCH[i]);
            float x = Mth.cos(STAR_YAW[i]) * cy * d;
            float y = Mth.sin(STAR_PITCH[i]) * d;
            float z = Mth.sin(STAR_YAW[i]) * cy * d;

            float blink = 0.45F + 0.55F * Mth.sin(time * 0.18F + STAR_BLINK[i]);
            spark(pose, vc, x, y, z, STAR_SIZE[i] * radius, right, up,
                    1.0F, 0.97F, 0.9F, blink);
        }
    }

    /**
     * 全方向から球へ集まってくる光の粒。
     *
     * 最初は1粒も無く、ためが進むにつれて数が増えていく。
     * 動きも最初はほとんど止まって見えるほど遅く、終盤で一気に速くなる。
     * 上下も含めた全方向から寄ってくるので、球が空間ごと吸い込んでいるように見える。
     */
    private static void drawIncomingLights(PoseStack.Pose pose, VertexConsumer vc, float radius,
                                           float charge, float time, Vector3f right, Vector3f up) {
        int count = Mth.floor(LIGHTS * charge * charge);
        if (count <= 0) return;

        // 序盤はほぼ止まって見えるくらい遅く、終盤で一気に加速する
        float speed = 0.06F + charge * charge * charge * 2.6F;

        for (int i = 0; i < count; i++) {
            float t = frac(LIGHT_PHASE[i] + time * 0.004F * LIGHT_SPEED[i] * speed);

            float dist = Mth.lerp(t, radius * LIGHT_START[i], radius * 1.02F);
            // 落ちながら少し巻き込まれる。まっすぐだと吸引に見えない
            float yaw = LIGHT_YAW[i] + t * 1.2F;
            float cy = Mth.cos(LIGHT_PITCH[i]);

            float x = Mth.cos(yaw) * cy * dist;
            float y = Mth.sin(LIGHT_PITCH[i]) * dist;
            float z = Mth.sin(yaw) * cy * dist;

            // 現れるところと吸い込まれるところで急に出入りしないようにする
            float alpha = Mth.clamp(t * 6.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) * 8.0F, 0.0F, 1.0F);

            spark(pose, vc, x, y, z, LIGHT_SIZE[i] * radius, right, up,
                    1.0F, 0.95F, 0.82F, alpha);
        }
    }

    // ---------------- はじけた後 ----------------

    /**
     * ビッグバン。集めた光が一気に外へばらまかれる。
     *
     * 出だしは目で追えないほど速く、そこから急激に減速して、
     * 最後は粉雪のようにゆっくり落ちながら消えていく。
     * 到達距離を粒ごとにばらけさせているので、外周に丸い線が見えない。
     */
    private void drawBigBang(PoseStack pose, MultiBufferSource buffer, MegiddoEntity entity,
                             float radius, float bang, float time, Vector3f right, Vector3f up) {

        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW_TEX));
        PoseStack.Pose last = pose.last();
        float reach = entity.getBlastRadius();

        // 中心の閃光。一瞬だけ強く光って消える
        float flashAlpha = Math.max(0.0F, 1.0F - bang * 4.0F);
        if (flashAlpha > 0.0F) {
            spark(last, glow, 0, 0, 0, radius * (3.0F + bang * 20.0F), right, up,
                    1.0F, 0.98F, 0.92F, flashAlpha);
        }

        // 飛び散る光。1 - e^(-kt) で「最初速く、あとは急減速」を作る
        float travel = 1.0F - (float) Math.exp(-4.5F * bang);

        for (int i = 0; i < BANG_MOTES; i++) {
            float maxDist = reach * BANG_DIST[i];
            float d = maxDist * travel;

            float cy = Mth.cos(BANG_PITCH[i]);
            float x = Mth.cos(BANG_YAW[i]) * cy * d;
            float z = Mth.sin(BANG_YAW[i]) * cy * d;
            // 勢いが落ちたぶんだけ下へ。粉雪のように舞い落ちる
            float y = Mth.sin(BANG_PITCH[i]) * d - BANG_FALL[i] * bang * bang * maxDist * 0.55F;

            // 消え際は瞬きながら薄れる。一斉に消えると板が消えたように見える
            float twinkle = 0.6F + 0.4F * Mth.sin(time * 0.7F + i);
            float alpha = (1.0F - bang) * (1.0F - bang) * twinkle;

            spark(last, glow, x, y, z, BANG_SIZE[i] * radius, right, up,
                    1.0F, 0.94F, 0.80F, alpha);
        }
    }

    // ---------------- 補助 ----------------

    /**
     * カメラの方を向く小さな板を1枚。
     *
     * 粒ごとに行列を積むと数が増えたときに重いので、
     * あらかじめ求めたカメラの右方向・上方向から四隅を直接作っている。
     */
    private static void spark(PoseStack.Pose pose, VertexConsumer vc,
                              float x, float y, float z, float size,
                              Vector3f right, Vector3f up,
                              float r, float g, float b, float a) {
        if (a <= 0.01F || size <= 0.0F) return;

        float h = size * 0.5F;
        float rx = right.x * h, ry = right.y * h, rz = right.z * h;
        float ux = up.x * h, uy = up.y * h, uz = up.z * h;

        VfxRenderUtil.quadLit(pose, vc, r, g, b, a, VfxRenderUtil.FULL_BRIGHT,
                x - rx - ux, y - ry - uy, z - rz - uz, 0.0F, 0.0F,
                x + rx - ux, y + ry - uy, z + rz - uz, 1.0F, 0.0F,
                x + rx + ux, y + ry + uy, z + rz + uz, 1.0F, 1.0F,
                x - rx + ux, y - ry + uy, z - rz + uz, 0.0F, 1.0F);
    }

    /** 0〜1 に収める小数部。負の値でも正しく回るようにしている */
    private static float frac(float v) {
        float f = v % 1.0F;
        return f < 0 ? f + 1.0F : f;
    }

    @Override
    public ResourceLocation getTextureLocation(MegiddoEntity entity) {
        return RING_TEX;
    }
}
