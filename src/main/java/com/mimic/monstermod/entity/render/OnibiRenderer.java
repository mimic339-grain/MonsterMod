package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.obj.OnibiEntity;
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
 * 鬼火の描画。
 *
 * 【粒を散らす方式をやめた理由】
 * 丸い光を散らして重ねる方式では、輪郭がぼやけて「炎の形」にならなかった。
 * 加算合成で中心が真っ白に飛び、色も形も失われてしまう。
 *
 * 【炎の輪郭そのものを作る】
 * 下から上へ「その高さでの幅」を決める関数を用意し、
 * その幅にそって帯を積み上げることで炎の形を作っている。
 *   下は円弧で丸く膨らませる → 火の玉らしい丸み
 *   上は先細りにして尖らせる → 炎の舌
 * 幅の中心を時間で横へずらすと、炎が揺らめく。
 * 上へ行くほど大きくずらすので、根元は据わったまま先だけが揺れる。
 *
 * 【色の重なり】
 * 同じ形を大きさを変えて3回重ねている(外側の青 → 中間 → 白い芯)。
 * 加算合成なので中心ほど明るくなり、参考画像のように芯が白く縁が色付く。
 *
 * 【横方向のぼかし】
 * 幅の方向にテクスチャの明るい中心から暗い縁までを貼っている。
 * 加算合成では暗い部分が描かれないので、これだけで縁がやわらかく溶ける。
 *
 * この描画設定は裏面を捨てるため、面は表裏1回ずつ描いている。
 */
public class OnibiRenderer extends EntityRenderer<OnibiEntity> {

    /** 中心が明るく外へ向かって消える丸。横方向のぼかしに使う */
    private static final ResourceLocation TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");

    /** 炎の高さ(ブロック) */
    private static final float HEIGHT = 1.9F;
    /** 炎の一番太いところの半幅(ブロック) */
    private static final float WIDTH = 0.62F;

    /** 縦の分割数。多いほど輪郭がなめらかになる */
    private static final int STEPS = 22;
    /** 脇に生える小さな炎の数 */
    private static final int SIDE_FLAMES = 4;

    // 色。外側が濃い青、中間が水色、芯が白
    private static final float[] COLOR_OUT  = { 0.15F, 0.40F, 1.00F };
    private static final float[] COLOR_MID  = { 0.40F, 0.80F, 1.00F };
    private static final float[] COLOR_CORE = { 0.95F, 1.00F, 1.00F };

    /** 幅の方向に貼るUV。中心が明るく、両端は暗い=透明になる */
    private static final float U0 = 0.14F, U1 = 0.86F, V_MID = 0.5F;

    // --- 脇の炎のばらつき ---
    private static final float[] SIDE_X = new float[SIDE_FLAMES];
    private static final float[] SIDE_Y = new float[SIDE_FLAMES];
    private static final float[] SIDE_SCALE = new float[SIDE_FLAMES];
    private static final float[] SIDE_PHASE = new float[SIDE_FLAMES];

    // --- 昇っていく粒 ---
    private static final int DROPS = 12;
    private static final float[] DROP_X = new float[DROPS];
    private static final float[] DROP_SIZE = new float[DROPS];
    private static final float[] DROP_PHASE = new float[DROPS];
    private static final float[] DROP_SPEED = new float[DROPS];

    static {
        Random rng = new Random(20260822L);
        for (int i = 0; i < SIDE_FLAMES; i++) {
            // 左右に振り分けて、根元の少し上から生やす
            SIDE_X[i] = (i % 2 == 0 ? -1.0F : 1.0F) * (0.34F + rng.nextFloat() * 0.28F);
            SIDE_Y[i] = 0.10F + rng.nextFloat() * 0.22F;
            SIDE_SCALE[i] = 0.34F + rng.nextFloat() * 0.24F;
            SIDE_PHASE[i] = rng.nextFloat() * 10.0F;
        }
        for (int i = 0; i < DROPS; i++) {
            DROP_X[i] = (rng.nextFloat() - 0.5F) * 0.55F;
            DROP_SIZE[i] = 0.09F + rng.nextFloat() * 0.13F;
            DROP_PHASE[i] = rng.nextFloat();
            DROP_SPEED[i] = 0.6F + rng.nextFloat() * 0.7F;
        }
    }

    public OnibiRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(OnibiEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {

        // 個体ごとに揺れをずらす。揃っていると弾幕にしたとき機械的に見える
        float time = (float) (entity.level().getGameTime() % 24000L) + partialTick
                + entity.getId() * 3.7F;

        // カメラの向きは1回だけ求めて使い回す
        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        Vector3f right = new Vector3f(1, 0, 0).rotate(cam);
        Vector3f up = new Vector3f(0, 1, 0).rotate(cam);

        pose.pushPose();
        // 炎は上へ伸びるので、当たり判定の中心より少し下から生やす
        pose.translate(0.0, -0.25, 0.0);
        PoseStack.Pose p = pose.last();

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(TEX));

        // 背後のぼんやりした光。これがあると空中に浮いている感じが出る
        blob(p, vc, 0.0F, HEIGHT * 0.32F, 0.0F, WIDTH * 3.4F, right, up,
                COLOR_OUT[0], COLOR_OUT[1], COLOR_OUT[2], 0.16F);

        // 脇の小さな炎。本体より先に描いて、本体に食い込ませる
        for (int i = 0; i < SIDE_FLAMES; i++) {
            float s = SIDE_SCALE[i];
            drawFlame(p, vc, right, up, time + SIDE_PHASE[i],
                    SIDE_X[i] * WIDTH, SIDE_Y[i] * HEIGHT,
                    HEIGHT * s, WIDTH * s * 0.85F,
                    COLOR_OUT, 0.26F);
            drawFlame(p, vc, right, up, time + SIDE_PHASE[i],
                    SIDE_X[i] * WIDTH, SIDE_Y[i] * HEIGHT,
                    HEIGHT * s * 0.6F, WIDTH * s * 0.5F,
                    COLOR_MID, 0.32F);
        }

        // 本体。同じ形を大きさを変えて3回重ね、中心ほど明るくする
        drawFlame(p, vc, right, up, time, 0.0F, 0.0F, HEIGHT, WIDTH, COLOR_OUT, 0.30F);
        drawFlame(p, vc, right, up, time, 0.0F, 0.0F, HEIGHT * 0.72F, WIDTH * 0.60F,
                COLOR_MID, 0.38F);
        drawFlame(p, vc, right, up, time, 0.0F, 0.0F, HEIGHT * 0.42F, WIDTH * 0.32F,
                COLOR_CORE, 0.55F);

        drawDrops(p, vc, time, right, up);

        pose.popPose();
    }

    /**
     * 炎を1つ描く。
     *
     * 下から上へ順に「その高さでの幅」を求め、帯をつないでいく。
     * 幅の中心を時間で横へずらすと揺らめき、上へ行くほどずらす量を増やすと
     * 根元は据わったまま先だけがなびく。
     */
    private static void drawFlame(PoseStack.Pose pose, VertexConsumer vc,
                                  Vector3f right, Vector3f up, float time,
                                  float baseX, float baseY,
                                  float height, float halfWidth,
                                  float[] color, float alpha) {

        float prevX = 0, prevY = 0, prevW = 0;

        for (int i = 0; i <= STEPS; i++) {
            float t = (float) i / STEPS;
            float w = profile(t) * halfWidth;
            float x = baseX + sway(t, time) * halfWidth;
            float y = baseY + t * height;

            if (i > 0) {
                quadBand(pose, vc, right, up, prevX, prevY, prevW, x, y, w,
                        color[0], color[1], color[2], alpha);
            }
            prevX = x; prevY = y; prevW = w;
        }
    }

    /**
     * その高さでの幅(0〜1)。
     * 下は円弧で丸く膨らませ、上は先細りにして尖らせる。
     * これが炎の形そのものになる。
     */
    private static float profile(float t) {
        final float bulb = 0.32F; // ここまでが丸い部分
        if (t <= bulb) {
            // 円弧。下端で0になり、一気に太くなるので丸く見える
            float k = (bulb - t) / bulb;
            return Mth.sqrt(Math.max(0.0F, 1.0F - k * k));
        }
        // 先細り。指数を1より大きくすると、上のほうで細くなって尖る
        float k = (t - bulb) / (1.0F - bulb);
        return (float) Math.pow(1.0F - k, 1.35);
    }

    /** 横へのずれ。上へ行くほど大きく揺れる */
    private static float sway(float t, float time) {
        float amount = t * t * 0.55F;
        return (Mth.sin(time * 0.26F + t * 4.2F) + 0.4F * Mth.sin(time * 0.41F + t * 7.5F))
                * amount;
    }

    /** 帯を1枚。幅の方向に明るい中心から暗い縁までを貼るので、縁がやわらかく溶ける */
    private static void quadBand(PoseStack.Pose pose, VertexConsumer vc,
                                 Vector3f right, Vector3f up,
                                 float x0, float y0, float w0,
                                 float x1, float y1, float w1,
                                 float r, float g, float b, float a) {
        if (a <= 0.01F) return;
        if (w0 <= 0.0001F && w1 <= 0.0001F) return;

        // カメラを向いた面の中で組み立てる
        float lx0 = x0 - w0, rx0 = x0 + w0;
        float lx1 = x1 - w1, rx1 = x1 + w1;

        VfxRenderUtil.quadBothSides(pose, vc, r, g, b, a,
                right.x * lx0 + up.x * y0, right.y * lx0 + up.y * y0, right.z * lx0 + up.z * y0, U0, V_MID,
                right.x * rx0 + up.x * y0, right.y * rx0 + up.y * y0, right.z * rx0 + up.z * y0, U1, V_MID,
                right.x * rx1 + up.x * y1, right.y * rx1 + up.y * y1, right.z * rx1 + up.z * y1, U1, V_MID,
                right.x * lx1 + up.x * y1, right.y * lx1 + up.y * y1, right.z * lx1 + up.z * y1, U0, V_MID);
    }

    /** 炎の先から離れて昇る粒。消えるとまた下から現れる */
    private static void drawDrops(PoseStack.Pose pose, VertexConsumer vc, float time,
                                  Vector3f right, Vector3f up) {
        for (int i = 0; i < DROPS; i++) {
            float t = (DROP_PHASE[i] + time * 0.018F * DROP_SPEED[i]) % 1.0F;
            if (t < 0) t += 1.0F;

            float x = (DROP_X[i] + Mth.sin(time * 0.4F + i) * 0.20F * t) * WIDTH;
            float y = HEIGHT * (0.55F + t * 0.85F);

            // 現れるところと消えるところで急に出入りしないようにする
            float alpha = Mth.clamp(t * 5.0F, 0.0F, 1.0F) * (1.0F - t) * 0.75F;

            blob(pose, vc, x, y, 0.0F, DROP_SIZE[i] * (1.0F - t * 0.4F), right, up,
                    COLOR_MID[0], COLOR_MID[1], COLOR_MID[2], alpha);
        }
    }

    /** カメラの方を向く丸い光を1つ */
    private static void blob(PoseStack.Pose pose, VertexConsumer vc,
                             float x, float y, float z, float size,
                             Vector3f right, Vector3f up,
                             float r, float g, float b, float a) {
        if (a <= 0.01F || size <= 0.0F) return;

        float h = size * 0.5F;
        float rx = right.x * h, ry = right.y * h, rz = right.z * h;
        float ux = up.x * h, uy = up.y * h, uz = up.z * h;

        // 位置もカメラを向いた面の中で組み立てる
        float px = right.x * x + up.x * y, py = right.y * x + up.y * y, pz = right.z * x + up.z * y;

        VfxRenderUtil.quadBothSides(pose, vc, r, g, b, a,
                px - rx - ux, py - ry - uy, pz - rz - uz, 0.0F, 0.0F,
                px + rx - ux, py + ry - uy, pz + rz - uz, 1.0F, 0.0F,
                px + rx + ux, py + ry + uy, pz + rz + uz, 1.0F, 1.0F,
                px - rx + ux, py - ry + uy, pz - rz + uz, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(OnibiEntity entity) {
        return TEX;
    }
}
