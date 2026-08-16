package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.obj.MegiddoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * 【すべてを1つの描画設定・1枚のテクスチャで描く理由】
 * 以前は球・円盤・光をそれぞれ別の描画設定で出していたが、
 * 円盤の手前半分が描かれない、光が1粒も出ない、という不具合が取れなかった。
 * バニラは大半の描画設定で1つの入れ物を使い回しており、
 * 設定を切り替えるたびにそこまでの内容を描いて入れ物を作り直す作りになっている。
 * その切り替えが絡むと再現しづらい取りこぼしが起きるため、
 * 切り替えを一切行わない形に作り直した。
 *
 * 【円盤を細かい四角の集まりで作る理由】
 * 大きな面を数枚並べる方式では手前半分が描かれなかったのに対し、
 * 同じ場所に出していた小さな板は問題なく描けていた。
 * そこで円盤も、細かい四角を全周・内外に敷き詰めて1枚のなめらかな面として作っている。
 *
 * 【構成】
 *  黒い球   … 光を通さない不透明の球。背景より暗くなり、穴が空いているように見える。
 *  円盤     … 浅く傾けた輪。濃淡の帯を持つ、つながった面。
 *  黒いチリ … 円盤の面に沿って外から球へ流れ込む細かい粒。
 *  黒いチリ(全方向) … 全方向(360度)から球へ集まってくる。ためが進むほど増えて速くなる。
 *  はじける … 集めたものが光となって一気に外へばらまかれ、
 *             減速しながら粉雪のように落ちて消える。色が出るのはこの瞬間だけ。
 */
public class MegiddoRenderer extends EntityRenderer<MegiddoEntity> {

    /**
     * 粒1つぶんの絵。
     *
     * 【専用のものを用意した理由】
     * ビームで使っている絵はぼかしを色(RGB)側に持たせてあり、加算合成でしか正しく出ない。
     * 通常の半透明で貼ると、ぼけた部分が「黒」としてそのまま出るため四角い板に見えてしまう。
     * こちらはぼかしをアルファに持たせてあるので、半透明で貼っても丸い粒になる。
     * さらに中心の数ピクセルだけ完全な不透明にしてあり、
     * 球や円盤の「面」はその一点だけを貼って塗りつぶしとして使っている。
     */
    private static final ResourceLocation TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/spark.png");

    // 絵は左右2つに分かれている。
    //   左半分 : やわらかくぼけた丸(集まってくる粒・星・砂粒)
    //   右半分 : 縁の立ったくっきりした丸(はじけたときのキラキラ)
    /** やわらかい粒のUV範囲 */
    private static final float SOFT_U0 = 0.0F, SOFT_U1 = 0.5F;
    /** くっきりした粒のUV範囲 */
    private static final float SHARP_U0 = 0.5F, SHARP_U1 = 1.0F;
    /** 面を塗りつぶすときに使う、左半分の中心(完全な不透明)のUV */
    private static final float FILL_U0 = 0.24F, FILL_U1 = 0.26F;
    private static final float FILL_V0 = 0.49F, FILL_V1 = 0.51F;

    /** 円盤の傾き。土星のように地面に近い浅い角度にする */
    private static final float RING_TILT = (float) Math.toRadians(14.0);

    /** 球の分割数 */
    private static final int SPHERE_LON = 30;
    private static final int SPHERE_LAT = 16;

    /** 円盤の内側/外側の半径(球の半径に対する倍率) */
    private static final float RING_INNER = 1.6F;
    private static final float RING_OUTER = 3.4F;
    /**
     * 円盤の面の分割数。細かい四角を敷き詰めて1枚の円盤にする。
     * 段数を多めに取っているのは、濃淡の変化をなめらかに出すため
     * (少ないと縞が階段状に見える)。
     */
    private static final int RING_SEGMENTS = 96;
    private static final int RING_BANDS = 40;

    /**
     * はじけたときに飛び散る光の色。
     * 1色だと寂しいので、白を基準にしつつ金・水色・紫・桃・緑を混ぜている。
     * ためている間に集まってくるのは黒い粒なので、色を使うのはこの瞬間だけ。
     */
    private static final float[][] LIGHT_COLORS = {
            { 1.00F, 0.97F, 0.90F }, // 白
            { 1.00F, 0.82F, 0.35F }, // 金
            { 0.55F, 0.88F, 1.00F }, // 水色
            { 0.78F, 0.60F, 1.00F }, // 紫
            { 1.00F, 0.58F, 0.72F }, // 桃
            { 0.60F, 1.00F, 0.78F }  // 緑
    };

    // 粒はチリのように細かくしてあるので、数を多めに取らないと流れとして見えない
    private static final int DUST = 520;    // 円盤沿いに流れ込む黒いチリ
    private static final int LIGHTS = 950;  // 全方向から集まる黒いチリ
    private static final int BANG = 2600;   // はじけたときにばらまく光

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

    // --- はじけたときの粒 ---
    private static final float[] BANG_YAW = new float[BANG];
    private static final float[] BANG_PITCH = new float[BANG];
    private static final float[] BANG_DIST = new float[BANG];
    private static final float[] BANG_SIZE = new float[BANG];
    private static final float[] BANG_FALL = new float[BANG];
    private static final int[] BANG_COLOR = new int[BANG];

    static {
        Random rng = new Random(20260819L);

        for (int i = 0; i < DUST; i++) {
            DUST_ANGLE[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            DUST_HEIGHT[i] = (rng.nextFloat() - 0.5F) * 0.35F;
            DUST_PHASE[i] = rng.nextFloat();
            DUST_SPEED[i] = 0.6F + rng.nextFloat() * 0.8F;
            // チリなので、ひと粒はごく小さくする
            DUST_SIZE[i] = 0.04F + rng.nextFloat() * 0.07F;
        }
        for (int i = 0; i < LIGHTS; i++) {
            LIGHT_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            // 上下も含めた全方向に散らす。偏らないよう緯度は正弦で分布させる
            LIGHT_PITCH[i] = (float) Math.asin(rng.nextFloat() * 2.0F - 1.0F);
            LIGHT_PHASE[i] = rng.nextFloat();
            LIGHT_SPEED[i] = 0.7F + rng.nextFloat() * 0.7F;
            LIGHT_START[i] = 3.6F + rng.nextFloat() * 3.4F;
            // チリなので、ひと粒はごく小さくする
            LIGHT_SIZE[i] = 0.035F + rng.nextFloat() * 0.055F;
        }
        for (int i = 0; i < BANG; i++) {
            BANG_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            BANG_PITCH[i] = (float) Math.asin(rng.nextFloat() * 2.0F - 1.0F);
            // 到達距離をばらけさせる。揃えると境界に丸い線が見えてしまう
            BANG_DIST[i] = 0.35F + rng.nextFloat() * rng.nextFloat() * 2.60F;
            BANG_SIZE[i] = 0.16F + rng.nextFloat() * 0.38F;
            BANG_FALL[i] = 0.5F + rng.nextFloat() * 1.2F;
            BANG_COLOR[i] = rng.nextFloat() < 0.30F ? 0 : rng.nextInt(LIGHT_COLORS.length);
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
        PoseStack.Pose p = pose.last();

        // ここから最後まで、描画設定を切り替えずに一気に出し切る
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEX));

        if (!banging) {
            drawBlackSphere(p, vc, radius);
            drawRingDisc(p, vc, radius, time);
            drawDust(p, vc, radius, charge, time, right, up);
            drawIncomingDust(p, vc, radius, charge, time, right, up);
        } else {
            drawBigBang(p, vc, entity, radius, bang, time, right, up);
        }

        pose.popPose();
    }

    /**
     * 光を通さない黒い球。
     * 不透明に描くことで背景より暗い「穴」に見せている。
     * この球だけは奥行きを書き込むので、裏側にある粒はきちんと隠れる。
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

                // 真っ黒。粒用の絵の中心(不透明な部分)だけを使う
                VfxRenderUtil.quadLit(pose, vc, 0.015F, 0.015F, 0.025F, 1.0F,
                        VfxRenderUtil.FULL_BRIGHT,
                        x00, y0, z00, FILL_U0, FILL_V0,
                        x10, y0, z10, FILL_U1, FILL_V0,
                        x11, y1, z11, FILL_U1, FILL_V1,
                        x01, y1, z01, FILL_U0, FILL_V1);
            }
        }
    }

    /**
     * 土星の環のような円盤。ちゃんと1枚の面としてつながった輪にする。
     *
     * 細かい四角を全周・内外に敷き詰めて作っている。
     * 大きな面を数枚で済ませると描き漏らしが起きたため、
     * 確実に出せている粒と同じくらいの大きさの面を並べる形にした。
     * 濃淡の帯(カッシーニの間隙のようなもの)は面の不透明度で表している。
     */
    private static void drawRingDisc(PoseStack.Pose pose, VertexConsumer vc, float radius,
                                     float time) {
        float inner = radius * RING_INNER;
        float outer = radius * RING_OUTER;
        float spin = time * 0.004F;
        float sinT = Mth.sin(RING_TILT), cosT = Mth.cos(RING_TILT);

        for (int b = 0; b < RING_BANDS; b++) {
            float t0 = (float) b / RING_BANDS;
            float t1 = (float) (b + 1) / RING_BANDS;
            float d0 = Mth.lerp(t0, inner, outer);
            float d1 = Mth.lerp(t1, inner, outer);

            float alpha = ringAlpha((t0 + t1) * 0.5F);
            if (alpha <= 0.12F) continue; // 薄すぎる帯は捨てられるので最初から出さない

            for (int s = 0; s < RING_SEGMENTS; s++) {
                float a0 = (float) (Math.PI * 2.0 * s / RING_SEGMENTS) + spin;
                float a1 = (float) (Math.PI * 2.0 * (s + 1) / RING_SEGMENTS) + spin;

                float c0 = Mth.cos(a0), n0 = Mth.sin(a0);
                float c1 = Mth.cos(a1), n1 = Mth.sin(a1);

                // 傾きは行列ではなく座標へ直接掛ける(Z軸まわり)
                float x00 = c0 * d0 * cosT, y00 = c0 * d0 * sinT, z00 = n0 * d0;
                float x10 = c1 * d0 * cosT, y10 = c1 * d0 * sinT, z10 = n1 * d0;
                float x01 = c0 * d1 * cosT, y01 = c0 * d1 * sinT, z01 = n0 * d1;
                float x11 = c1 * d1 * cosT, y11 = c1 * d1 * sinT, z11 = n1 * d1;

                // 表と裏。円盤は薄いので、両側から見えないと消えたように見える
                VfxRenderUtil.quadLit(pose, vc, 0.96F, 0.92F, 0.74F, alpha,
                        VfxRenderUtil.FULL_BRIGHT,
                        x00, y00, z00, FILL_U0, FILL_V0,
                        x10, y10, z10, FILL_U1, FILL_V0,
                        x11, y11, z11, FILL_U1, FILL_V1,
                        x01, y01, z01, FILL_U0, FILL_V1);
                VfxRenderUtil.quadLit(pose, vc, 0.96F, 0.92F, 0.74F, alpha,
                        VfxRenderUtil.FULL_BRIGHT,
                        x01, y01, z01, FILL_U0, FILL_V1,
                        x11, y11, z11, FILL_U1, FILL_V1,
                        x10, y10, z10, FILL_U1, FILL_V0,
                        x00, y00, z00, FILL_U0, FILL_V0);
            }
        }
    }

    /** 円盤の濃淡。同心の縞と数本の暗い隙間、内外の縁の薄まりを合わせたもの */
    private static float ringAlpha(float t) {
        float band = 0.62F + 0.38F * Mth.sin(t * 26.0F);
        float gap = Mth.abs(Mth.sin(t * 7.3F + 1.2F)) < 0.10F ? 0.30F : 1.0F;
        float edge = Mth.sin((float) Math.PI * Mth.clamp(t, 0.0F, 1.0F));
        return band * gap * (0.30F + 0.70F * edge);
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
        float sinT = Mth.sin(RING_TILT), cosT = Mth.cos(RING_TILT);

        for (int i = 0; i < DUST; i++) {
            float t = frac(DUST_PHASE[i] + time * 0.004F * DUST_SPEED[i] * speed);

            float dist = Mth.lerp(t, radius * RING_OUTER, radius * 0.95F);
            float ang = DUST_ANGLE[i] + t * 1.6F;             // 巻き込まれながら落ちる
            float fy = DUST_HEIGHT[i] * radius * (1.0F - t);  // 球に近づくほど平らになる

            float fx = Mth.cos(ang) * dist;
            float fz = Mth.sin(ang) * dist;
            float x = fx * cosT - fy * sinT;
            float y = fx * sinT + fy * cosT;

            float alpha = Mth.clamp((1.0F - t) * 3.0F, 0.0F, 1.0F) * 0.9F;
            spark(pose, vc, x, y, fz, DUST_SIZE[i] * radius * 0.5F, right, up,
                    0.02F, 0.02F, 0.04F, alpha);
        }
    }

    /**
     * 全方向から球へ集まってくる黒い粒。
     *
     * 最初は1粒も無く、ためが進むにつれて数が増えていく。
     * 動きも最初はほとんど止まって見えるほど遅く、終盤で一気に速くなる。
     * ためている間は空間ごと吸い込んでいる様子を出したいので黒。
     * 光るのは、はじけて放出されるときだけにしてある。
     */
    private static void drawIncomingDust(PoseStack.Pose pose, VertexConsumer vc, float radius,
                                           float charge, float time, Vector3f right, Vector3f up) {
        int count = Mth.floor(LIGHTS * charge * charge);
        if (count <= 0) return;

        // 序盤はほぼ止まって見えるくらい遅く、終盤で一気に加速する
        float speed = 0.06F + charge * charge * charge * 2.6F;

        for (int i = 0; i < count; i++) {
            float t = frac(LIGHT_PHASE[i] + time * 0.004F * LIGHT_SPEED[i] * speed);

            float dist = Mth.lerp(t, radius * LIGHT_START[i], radius * 1.08F);
            float yaw = LIGHT_YAW[i] + t * 1.2F; // 少し巻き込まれる。まっすぐだと吸引に見えない
            float cy = Mth.cos(LIGHT_PITCH[i]);

            float x = Mth.cos(yaw) * cy * dist;
            float y = Mth.sin(LIGHT_PITCH[i]) * dist;
            float z = Mth.sin(yaw) * cy * dist;

            // 現れるところと吸い込まれるところで急に出入りしないようにする
            float alpha = Mth.clamp(t * 6.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) * 8.0F, 0.0F, 1.0F);
            spark(pose, vc, x, y, z, LIGHT_SIZE[i] * radius, right, up,
                    0.02F, 0.02F, 0.04F, alpha);
        }
    }

    /**
     * ビッグバン。集めた光が一気に外へばらまかれる。
     *
     * 出だしは目で追えないほど速く、そこから急激に減速して、
     * 最後は粉雪のようにゆっくり落ちながら消えていく。
     * 到達距離を粒ごとにばらけさせているので、外周に丸い線が見えない。
     */
    private static void drawBigBang(PoseStack.Pose pose, VertexConsumer vc, MegiddoEntity entity,
                                    float radius, float bang, float time,
                                    Vector3f right, Vector3f up) {
        float reach = entity.getBlastRadius();

        // 中心の閃光。一瞬だけ強く光って消える
        float flash = Math.max(0.0F, 1.0F - bang * 4.0F);
        if (flash > 0.0F) {
            spark(pose, vc, 0, 0, 0, radius * (3.0F + bang * 20.0F), right, up,
                    1.0F, 0.98F, 0.92F, flash);
        }

        // 飛び散る光。1 - e^(-kt) で「最初速く、あとは急減速」を作る
        float travel = 1.0F - (float) Math.exp(-4.5F * bang);

        for (int i = 0; i < BANG; i++) {
            float maxDist = reach * BANG_DIST[i];
            float d = maxDist * travel;

            float cy = Mth.cos(BANG_PITCH[i]);
            float x = Mth.cos(BANG_YAW[i]) * cy * d;
            float z = Mth.sin(BANG_YAW[i]) * cy * d;
            // 勢いが落ちたぶんだけ下へ。粉雪のように舞い落ちる
            float y = Mth.sin(BANG_PITCH[i]) * d - BANG_FALL[i] * bang * bang * maxDist * 0.55F;

            // 消え際は瞬きながら薄れる。一斉に消えると板が消えたように見える。
            // 振れ幅を大きく取って、粒ごとにチカチカするようにしている
            float twinkle = 0.45F + 0.55F * Mth.sin(time * 1.1F + i * 1.7F);
            float alpha = (1.0F - bang) * (1.0F - bang) * twinkle;

            float[] col = LIGHT_COLORS[BANG_COLOR[i]];
            // ぼかしの少ない方の絵を使い、粒立ちのはっきりしたキラキラにする
            spark(pose, vc, x, y, z, BANG_SIZE[i] * radius, right, up,
                    col[0], col[1], col[2], alpha, SHARP_U0, SHARP_U1);
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
        spark(pose, vc, x, y, z, size, right, up, r, g, b, a, SOFT_U0, SOFT_U1);
    }

    /** 粒の絵をどちらの半分から取るか指定できる版 */
    private static void spark(PoseStack.Pose pose, VertexConsumer vc,
                              float x, float y, float z, float size,
                              Vector3f right, Vector3f up,
                              float r, float g, float b, float a,
                              float u0, float u1) {
        // この描画設定は不透明度が一定を下回った点を捨てるので、薄すぎるものは出さない
        if (a <= 0.12F || size <= 0.0F) return;

        float h = size * 0.5F;
        float rx = right.x * h, ry = right.y * h, rz = right.z * h;
        float ux = up.x * h, uy = up.y * h, uz = up.z * h;

        VfxRenderUtil.quadLit(pose, vc, r, g, b, a, VfxRenderUtil.FULL_BRIGHT,
                x - rx - ux, y - ry - uy, z - rz - uz, u0, 0.0F,
                x + rx - ux, y + ry - uy, z + rz - uz, u1, 0.0F,
                x + rx + ux, y + ry + uy, z + rz + uz, u1, 1.0F,
                x - rx + ux, y - ry + uy, z - rz + uz, u0, 1.0F);
    }

    /** 0〜1 に収める小数部。負の値でも正しく回るようにしている */
    private static float frac(float v) {
        float f = v % 1.0F;
        return f < 0 ? f + 1.0F : f;
    }

    @Override
    public ResourceLocation getTextureLocation(MegiddoEntity entity) {
        return TEX;
    }
}
