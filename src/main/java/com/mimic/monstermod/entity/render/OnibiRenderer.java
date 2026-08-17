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
 * 【加算合成をやめた理由 = 白飛びの原因】
 * 以前は炎全体を {@code RenderType.eyes}(加算合成)で描いていた。
 * 加算合成は「後ろの色に足す」ので、重ねるほど必ず白へ近づき、
 * どれだけ濃い紫や藍を指定しても最終的にただの白い光の塊になる。
 * 濃い色・暗い色は加算合成では原理的に表現できない。
 *
 * そこで竜巻の本体と同じ {@code RenderType.entityNoOutline}(通常の半透明合成)へ変更した。
 * 半透明合成は「指定した色で塗る」ので、濃い紫は濃い紫のまま出る。
 * 光らせるのは中心のごく小さい芯だけで、そこだけ加算合成で描いている。
 *
 * 【テクスチャを spark.png に変えた理由】
 * beam_glow.png は明暗をRGBで表現しアルファは全面255。
 * 加算合成では黒い部分が描かれないので丸く見えるが、
 * 半透明合成にすると「黒い四角」がそのまま出てしまう。
 * spark.png は同じぼかしをアルファ側に持っているので半透明合成でも丸く抜ける。
 * 左半分(u 0.0〜0.5)がやわらかいぼかし、右半分は輪郭のはっきりした版。
 *
 * 半透明合成は裏面も描く設定なので、面は quad(1回)でよい。
 * 芯だけは加算合成で裏面が捨てられるため quadBothSides を使う。
 */
public class OnibiRenderer extends EntityRenderer<OnibiEntity> {

    /** 炎の本体。アルファ側にぼかしを持つので半透明合成でも丸く抜ける */
    private static final ResourceLocation TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/spark.png");

    /** 中心の芯だけに使う加算合成用のぼかし(明暗をRGBで持つ) */
    private static final ResourceLocation CORE_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");

    // 【縦横比について】
    // 数値上の高さ:全幅は 3.8:2.48 とそこまで極端ではなかったが、
    // profile() が早い段階から先細りするため、上の2/3がほとんど幅を持たず、
    // 見た目は細い槍のようになっていた。
    // 高さを詰めて幅を広げるだけでなく、profile() 側の膨らみも合わせて広げている。

    /** 炎の高さ(ブロック) */
    private static final float HEIGHT = 2.7F;
    /** 炎の一番太いところの半幅(ブロック)。全幅はこの2倍 */
    private static final float WIDTH = 1.45F;

    /** 縦の分割数。多いほど輪郭がなめらかになる */
    private static final int STEPS = 22;

    // 色。外から内へ 藍 → 青 → 紫 と変えていく。
    //
    // 半透明合成なのでここに書いた色がほぼそのまま画面に出る。
    // 加算合成のときのように「重ねると白へ寄る」ことがないため、
    // 見本の絵と同じように暗くて濃い色をそのまま指定できる。
    private static final float[] COLOR_OUTER  = { 0.04F, 0.05F, 0.30F }; // 濃い藍(一番外)
    private static final float[] COLOR_BLUE   = { 0.08F, 0.22F, 0.85F }; // 濃い青
    private static final float[] COLOR_PURPLE = { 0.30F, 0.05F, 0.62F }; // 濃い紫
    private static final float[] COLOR_CORE   = { 0.70F, 0.75F, 1.00F }; // 芯(ここだけ光る)

    /**
     * 幅の方向に貼るUV。
     * spark.png の左半分(u 0.0〜0.5)がやわらかいぼかしなので、その範囲だけを使う。
     * u=0.25 がアルファ最大(中心)、両端に向かってアルファが0になる。
     */
    private static final float U0 = 0.02F, U1 = 0.48F, V_MID = 0.5F;

    // --- 昇っていく粒 ---
    private static final int DROPS = 12;
    private static final float[] DROP_X = new float[DROPS];
    private static final float[] DROP_SIZE = new float[DROPS];
    private static final float[] DROP_PHASE = new float[DROPS];
    private static final float[] DROP_SPEED = new float[DROPS];

    static {
        Random rng = new Random(20260822L);
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
        // (炎を低くしたので、下げる量も合わせて浅くしている)
        pose.translate(0.0, -0.40, 0.0);
        PoseStack.Pose p = pose.last();

        // --- 炎の本体。半透明合成なので、指定した濃い色がそのまま出る ---
        // 後から描いたものが上に乗るので、外→内の順に描いて内側の色を手前に見せる。
        // 背後の大きなぼんやり光は、周りまで白く染めていた原因なので出さない。
        VertexConsumer body = buffer.getBuffer(RenderType.entityNoOutline(TEX));

        // 一番外が一番高く太いので、内側の層は必ずこの中に収まり、
        // アホ毛(先端)も一番外の1本だけが延長線上に伸びる
        drawFlame(p, body, right, up, time, 0.0F, 0.0F,
                HEIGHT, WIDTH, COLOR_OUTER, 0.85F, U0, U1, false);
        drawFlame(p, body, right, up, time, 0.0F, 0.0F,
                HEIGHT * 0.88F, WIDTH * 0.80F, COLOR_BLUE, 0.90F, U0, U1, false);
        drawFlame(p, body, right, up, time, 0.0F, 0.0F,
                HEIGHT * 0.70F, WIDTH * 0.58F, COLOR_PURPLE, 0.95F, U0, U1, false);

        drawDrops(p, body, time, right, up);

        // --- 芯だけ加算合成で光らせる ---
        // 光るのはここだけ。大きくすると以前と同じ白飛びに戻るので小さく保つ。
        // beam_glow.png は明暗をRGBで持つので、UVは中心の明るいところを含む範囲を使う。
        // 加算合成は裏面を捨てるため表裏1回ずつ描く
        VertexConsumer core = buffer.getBuffer(RenderType.eyes(CORE_TEX));
        drawFlame(p, core, right, up, time, 0.0F, 0.0F,
                HEIGHT * 0.30F, WIDTH * 0.20F, COLOR_CORE, 0.30F, 0.14F, 0.86F, true);

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
                                  float[] color, float alpha,
                                  float u0, float u1, boolean bothSides) {

        float prevX = 0, prevY = 0, prevW = 0;

        for (int i = 0; i <= STEPS; i++) {
            float t = (float) i / STEPS;
            float w = profile(t) * halfWidth;
            float y = baseY + t * height;

            // 【重要】揺れは層ごとではなく、炎全体の高さと決まった幅を基準に求める。
            // 層ごとの高さや幅を掛けてしまうと、層によって曲がり方が変わり、
            // 先端がバラバラの方向へ伸びて何本もあるように見えてしまう。
            // 同じ中心線を共有させることで、外側の層が内側をきちんと包む
            float x = baseX + sway(y / HEIGHT, time) * WIDTH;

            if (i > 0) {
                quadBand(pose, vc, right, up, prevX, prevY, prevW, x, y, w,
                        color[0], color[1], color[2], alpha, u0, u1, bothSides);
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
        final float bulb = 0.40F; // ここまでが丸い部分。上げるほど丸い胴が長くなる
        if (t <= bulb) {
            // 円弧。下端で0になり、一気に太くなるので丸く見える
            float k = (bulb - t) / bulb;
            return Mth.sqrt(Math.max(0.0F, 1.0F - k * k));
        }
        // 先細り。
        // 指数を1より大きくすると早い段階から細くなって槍のようになる。
        // 1に近づけるほど幅を保ったまま上がり、先端だけが尖った火の玉らしい形になる
        float k = (t - bulb) / (1.0F - bulb);
        return (float) Math.pow(1.0F - k, 0.95);
    }

    /** 横へのずれ。上へ行くほど大きく揺れる */
    private static float sway(float t, float time) {
        float amount = t * t * 0.55F;
        return (Mth.sin(time * 0.26F + t * 4.2F) + 0.4F * Mth.sin(time * 0.41F + t * 7.5F))
                * amount;
    }

    /**
     * 帯を1枚。幅の方向にテクスチャの中心から縁までを貼るので、横の縁がやわらかく溶ける。
     * bothSides は加算合成(裏面が捨てられる)のときだけ true にする。
     * 半透明合成は裏面も描くので、true にすると同じ面を2回塗って濃くなってしまう。
     */
    private static void quadBand(PoseStack.Pose pose, VertexConsumer vc,
                                 Vector3f right, Vector3f up,
                                 float x0, float y0, float w0,
                                 float x1, float y1, float w1,
                                 float r, float g, float b, float a,
                                 float u0, float u1, boolean bothSides) {
        if (a <= 0.01F) return;
        if (w0 <= 0.0001F && w1 <= 0.0001F) return;

        // カメラを向いた面の中で組み立てる
        float lx0 = x0 - w0, rx0 = x0 + w0;
        float lx1 = x1 - w1, rx1 = x1 + w1;

        float ax = right.x * lx0 + up.x * y0, ay = right.y * lx0 + up.y * y0, az = right.z * lx0 + up.z * y0;
        float bx = right.x * rx0 + up.x * y0, by = right.y * rx0 + up.y * y0, bz = right.z * rx0 + up.z * y0;
        float cx = right.x * rx1 + up.x * y1, cy = right.y * rx1 + up.y * y1, cz = right.z * rx1 + up.z * y1;
        float dx = right.x * lx1 + up.x * y1, dy = right.y * lx1 + up.y * y1, dz = right.z * lx1 + up.z * y1;

        if (bothSides) {
            VfxRenderUtil.quadBothSides(pose, vc, r, g, b, a,
                    ax, ay, az, u0, V_MID, bx, by, bz, u1, V_MID,
                    cx, cy, cz, u1, V_MID, dx, dy, dz, u0, V_MID);
        } else {
            VfxRenderUtil.quad(pose, vc, r, g, b, a,
                    ax, ay, az, u0, V_MID, bx, by, bz, u1, V_MID,
                    cx, cy, cz, u1, V_MID, dx, dy, dz, u0, V_MID);
        }
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
            // 半透明合成になったので、以前の加算合成向けの薄さでは見えない。
            // 濃い紫の粒がはっきり浮くくらいの濃さにしている
            float alpha = Mth.clamp(t * 5.0F, 0.0F, 1.0F) * (1.0F - t) * 0.85F;

            blob(pose, vc, x, y, 0.0F, DROP_SIZE[i] * (1.0F - t * 0.4F), right, up,
                    COLOR_PURPLE[0], COLOR_PURPLE[1], COLOR_PURPLE[2], alpha);
        }
    }

    /**
     * カメラの方を向く丸い粒を1つ。
     * spark.png の左半分(u 0.0〜0.5)がやわらかいぼかしの丸なので、その範囲だけを貼る。
     * 半透明合成の設定は裏面も描くので、面は1回でよい。
     */
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

        VfxRenderUtil.quad(pose, vc, r, g, b, a,
                px - rx - ux, py - ry - uy, pz - rz - uz, 0.0F, 0.0F,
                px + rx - ux, py + ry - uy, pz + rz - uz, 0.5F, 0.0F,
                px + rx + ux, py + ry + uy, pz + rz + uz, 0.5F, 1.0F,
                px - rx + ux, py - ry + uy, pz - rz + uz, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(OnibiEntity entity) {
        return TEX;
    }
}
