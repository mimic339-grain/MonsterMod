package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.obj.VortexEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * 竜巻(渦)の描画。
 *
 * 【構造】内側に「煙の柱」、外側に「風切りの弧」。
 *
 * 竜巻の形は円錐そのものを見せて作るのではなく、
 * 大きさも高さもバラバラな弧(風切り)をたくさん重ねて
 * 「だいたいこの形」と分かる程度に象る。こうすると定型の円錐に見えない。
 * 弧の一部は煙の柱より外へはみ出すので、外周に時々スラッシュが走るように見える。
 *
 * 【形】下は細い柱、上へ行くほど急に広がり、頂上は皿のように開く。
 * 半径は r = 下半径 + (上半径 - 下半径) * t^FLARE で決まる。
 * FLARE を大きくすると下半分がほとんど広がらず、上で一気に開く
 * (この形にしないと、ただのコップに見えてしまう)。
 * さらに頂上付近には広がりを強調する専用の弧(皿の部分)を置いている。
 *
 * 【明るさ・色】加算合成は一切使わず、柱も弧も通常の半透明で描く。
 * 加算合成は光を足すことしかできないため背景より暗くできず、
 * 何枚も重なると必ず白飛びしてしまうため。
 * 色はほぼ黒に近い灰色で、ほとんど透けない濃さにしてある。
 *
 * 【描画設定の使い分け】
 *  柱 = {@code entityTranslucent}  … 深度バッファに書き込む
 *  弧 = {@code entityNoOutline}    … 深度バッファに書き込まない
 * 柱が深度を書き、弧はその後に描かれるので、
 * 柱の裏側にある弧は自動的に隠れる(濃い柱の向こうが透けて見えない)。
 * 弧同士は深度を書かないぶん重ね合わせが自由だが、
 * どちらの設定も奥から手前へ並べ替えてから描かれるので順番は破綻しない。
 * どちらも裏面を描く設定(NO_CULL)なので、面は1回ずつ出せばよい。
 */
public class VortexRenderer extends EntityRenderer<VortexEntity> {

    private static final ResourceLocation SLASH_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/vortex.png");
    private static final ResourceLocation SMOKE_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/smoke.png");

    /** 風切りの弧の枚数。うち末尾 CAP_SLASHES 枚は頂上の「皿」を作る */
    private static final int SLASHES = 40;
    private static final int CAP_SLASHES = 12;
    /** 弧1枚の分割数。少ないと折れ線に見える */
    private static final int ARC_STEPS = 20;

    /** 煙の柱の分割数。多角形に見えないよう多めに取る */
    private static final int BODY_SIDES = 32;
    private static final int BODY_RINGS = 14;
    /** 煙の柱は弧より内側に置く */
    private static final float BODY_SCALE = 0.72F;

    /**
     * 上へ向かう広がり方。大きいほど下が細長く、頂上で一気に開く。
     * 既存の TornadoParticle が pow(4) で竜巻らしい形になっていたので、それに近づけている。
     */
    private static final float FLARE = 3.6F;

    /**
     * 濃さ。どちらも通常の半透明なので、この値がそのまま不透明度になる。
     * 柱は手前の壁と奥の壁の2枚を通るので、0.85でも合わせるとほぼ透けない
     * (手前で85%隠れ、残りの15%も奥の壁で85%隠れる)。
     */
    private static final float ALPHA_BODY = 0.85F;
    private static final float ALPHA_SLASH = 0.95F;

    // 弧ごとのばらつき。毎フレーム同じ形になるよう固定の種から先に作っておく
    private static final float[] PHASE = new float[SLASHES];   // 開始角
    private static final float[] SPAN = new float[SLASHES];    // 弧の長さ(ラジアン)
    private static final float[] RAD = new float[SLASHES];     // 本体に対する半径の倍率
    private static final float[] WIDTH = new float[SLASHES];   // 弧の厚み(上半径に対する割合)
    private static final float[] RISE = new float[SLASHES];    // 昇る速さ
    private static final float[] SPIN = new float[SLASHES];    // 回る速さ
    private static final float[] Y0 = new float[SLASHES];      // 高さ(昇る弧では初期位置)
    private static final float[] TILT = new float[SLASHES];    // 弧に沿って昇る量(螺旋の強さ)
    private static final float[] BRIGHT = new float[SLASHES];
    private static final float[] FLOW = new float[SLASHES];    // UVが流れる速さ

    /** 頂上の皿を作る弧かどうか。こちらは昇らず、その高さに留まって回る */
    private static boolean isCap(int i) { return i >= SLASHES - CAP_SLASHES; }

    static {
        Random rng = new Random(20260818L);
        for (int i = 0; i < SLASHES; i++) {
            PHASE[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            // ほとんど透けない濃さで揃える(薄い弧が混ざると白っぽく霞んで見えるため)
            BRIGHT[i] = 0.80F + rng.nextFloat() * 0.20F;
            FLOW[i] = 0.02F + rng.nextFloat() * 0.06F;

            if (isCap(i)) {
                // 皿の部分: 頂上付近で、大きく広がった、ほぼ水平の弧
                SPAN[i] = 3.4F + rng.nextFloat() * 2.6F;       // 約195〜345度
                RAD[i] = 0.95F + rng.nextFloat() * 0.45F;
                WIDTH[i] = 0.012F + rng.nextFloat() * 0.030F;
                RISE[i] = 0.0F;                                 // 昇らず頂上に留まる
                SPIN[i] = 0.05F + rng.nextFloat() * 0.10F;      // 皿はゆっくり回す
                Y0[i] = 0.84F + rng.nextFloat() * 0.18F;
                TILT[i] = -0.02F + rng.nextFloat() * 0.06F;     // わずかに上下する程度
            } else {
                // 柱に巻き付く部分: 下から湧いて上へ昇る
                SPAN[i] = 1.8F + rng.nextFloat() * 3.8F;        // 約100〜320度
                // 1.0を超えるものが柱からはみ出して「外に走るスラッシュ」になる
                RAD[i] = 0.85F + rng.nextFloat() * 0.55F;
                WIDTH[i] = 0.010F + rng.nextFloat() * 0.045F;
                RISE[i] = 0.004F + rng.nextFloat() * 0.010F;
                SPIN[i] = 0.10F + rng.nextFloat() * 0.22F;
                if (rng.nextBoolean()) SPIN[i] *= 0.55F;        // 遅い弧を混ぜて動きを単調にしない
                Y0[i] = rng.nextFloat();
                TILT[i] = 0.02F + rng.nextFloat() * 0.10F;
            }
        }
    }

    public VortexRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VortexEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {

        float fade = entity.getFade(partialTick);
        if (fade <= 0.0F) return;

        float height = entity.getVortexHeight();
        float rBottom = entity.getBottomRadius();
        float rTop = entity.getTopRadius();

        int rgb = entity.getColor();
        float cr = ((rgb >> 16) & 0xFF) / 255.0F;
        float cg = ((rgb >> 8) & 0xFF) / 255.0F;
        float cb = (rgb & 0xFF) / 255.0F;

        // 全プレイヤーで同じ位相になるようワールド時間を使う
        float time = (float) (entity.level().getGameTime() % 24000L) + partialTick;

        pose.pushPose();

        // 先に柱を描く。こちらは深度を書くので、あとで描く弧のうち柱の裏側にある分は隠れる
        VertexConsumer body = buffer.getBuffer(RenderType.entityTranslucent(SMOKE_TEX));
        drawBody(pose, body, height, rBottom, rTop, cr, cg, cb, ALPHA_BODY * fade, time, packedLight);

        // 外側の風切り
        VertexConsumer slash = buffer.getBuffer(RenderType.entityNoOutline(SLASH_TEX));
        for (int i = 0; i < SLASHES; i++) {
            drawSlash(pose, slash, i, height, rBottom, rTop, cr, cg, cb, fade, time, packedLight);
        }

        pose.popPose();
    }

    /** 高さの割合(0〜1)から、その高さでの半径を求める */
    private static float radiusAt(float t, float rBottom, float rTop) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return rBottom + (rTop - rBottom) * (float) Math.pow(c, FLARE);
    }

    /**
     * 風切りの弧を1枚描く。
     *
     * 柱に巻き付く弧は下から湧いて上へ昇り、上端で消える。
     * 昇るにつれて半径も大きくなるので「巻き上がりながら広がる」動きになる。
     * 皿の弧は昇らず、頂上でゆっくり回り続ける。
     */
    private static void drawSlash(PoseStack pose, VertexConsumer vc, int i,
                                  float height, float rBottom, float rTop,
                                  float cr, float cg, float cb, float fade, float time,
                                  int packedLight) {

        float yNorm;
        float env;

        if (isCap(i)) {
            yNorm = Y0[i];
            env = 1.0F; // 頂上に留まるので出入りのフェードは不要
        } else {
            // 高さは時間で循環させる。下で現れて上で消える
            yNorm = (Y0[i] + time * RISE[i]) % 1.0F;
            if (yNorm < 0) yNorm += 1.0F;
            env = Mth.clamp(yNorm / 0.12F, 0.0F, 1.0F)
                    * Mth.clamp((1.0F - yNorm) / 0.30F, 0.0F, 1.0F);
            if (env <= 0.001F) return;
        }

        float baseAlpha = ALPHA_SLASH * BRIGHT[i] * env * fade;
        float spin = time * SPIN[i];
        float uScroll = time * FLOW[i];

        // 弧は柱よりわずかに明るい灰色にして、暗い柱の上でも風の筋として見分けられるようにする
        float sr = 0.16F + cr * 0.20F;
        float sg = 0.16F + cg * 0.20F;
        float sb = 0.17F + cb * 0.21F;

        PoseStack.Pose last = pose.last();

        float px = 0, py = 0, pz = 0, pw = 0, pu = 0;

        for (int s = 0; s <= ARC_STEPS; s++) {
            float f = (float) s / ARC_STEPS;

            // 弧に沿って少しずつ高さが変わる(完全な水平の輪にしない)
            float t = yNorm + TILT[i] * f;
            float y = Mth.clamp(t, 0.0F, 1.05F) * height;
            float r = radiusAt(t, rBottom, rTop) * RAD[i];

            float ang = PHASE[i] + spin + SPAN[i] * f;
            float x = Mth.cos(ang) * r;
            float z = Mth.sin(ang) * r;

            // 両端を細くして、先が消えていくようにする
            float taper = Mth.sin((float) Math.PI * f);
            float w = WIDTH[i] * rTop * (0.25F + 0.75F * taper);
            float u = f * SPAN[i] * 0.35F - uScroll;

            if (s > 0) {
                // 裏面も描かれる設定なので1回でよい
                float a = baseAlpha * (0.20F + 0.80F * taper);
                VfxRenderUtil.quadLit(last, vc, sr, sg, sb, a, packedLight,
                        px, py - pw, pz, pu, 0.0F,
                        x,  y  - w,  z,  u,  0.0F,
                        x,  y  + w,  z,  u,  1.0F,
                        px, py + pw, pz, pu, 1.0F);
            }

            px = x; py = y; pz = z; pw = w; pu = u;
        }
    }

    /**
     * 内側の煙の柱。通常の半透明で描くので、濃い灰色で背景を暗く覆える。
     *
     * この描画設定は裏面も描く(NO_CULL)ので、面は1回ずつ出せばよい。
     * 表裏2回出すと不透明度が二重になり、灰色の塊に見えてしまう。
     */
    private static void drawBody(PoseStack pose, VertexConsumer vc,
                                 float height, float rBottom, float rTop,
                                 float cr, float cg, float cb, float alpha,
                                 float time, int packedLight) {
        PoseStack.Pose last = pose.last();
        float spin = time * 0.04F;
        float vScroll = time * 0.05F;

        // ほぼ黒。色味はごくわずかに残す程度にする
        float br = 0.035F + cr * 0.055F;
        float bg = 0.035F + cg * 0.055F;
        float bb = 0.040F + cb * 0.060F;

        for (int ring = 0; ring < BODY_RINGS; ring++) {
            float t0 = (float) ring / BODY_RINGS;
            float t1 = (float) (ring + 1) / BODY_RINGS;
            float y0 = t0 * height, y1 = t1 * height;
            float r0 = radiusAt(t0, rBottom, rTop) * BODY_SCALE;
            float r1 = radiusAt(t1, rBottom, rTop) * BODY_SCALE;

            // 上端と下端をわずかに薄くする程度に留める。
            // ここで薄くしすぎると柱が透けてしまうし、この描画設定は
            // 不透明度が0.1を下回った部分を捨てるため、縁がギザギザに欠ける
            float mid = (t0 + t1) * 0.5F;
            float a = alpha * (0.55F + 0.45F * Mth.sin((float) Math.PI * Mth.clamp(mid, 0.0F, 1.0F)));

            for (int i = 0; i < BODY_SIDES; i++) {
                float ang0 = (float) (Math.PI * 2.0 * i / BODY_SIDES) + spin;
                float ang1 = (float) (Math.PI * 2.0 * (i + 1) / BODY_SIDES) + spin;

                float x00 = Mth.cos(ang0) * r0, z00 = Mth.sin(ang0) * r0;
                float x10 = Mth.cos(ang1) * r0, z10 = Mth.sin(ang1) * r0;
                float x01 = Mth.cos(ang0) * r1, z01 = Mth.sin(ang0) * r1;
                float x11 = Mth.cos(ang1) * r1, z11 = Mth.sin(ang1) * r1;

                float u0 = (float) i / BODY_SIDES;
                float u1 = (float) (i + 1) / BODY_SIDES;
                float v0 = t0 * 2.5F - vScroll;
                float v1 = t1 * 2.5F - vScroll;

                VfxRenderUtil.quadLit(last, vc, br, bg, bb, a, packedLight,
                        x00, y0, z00, u0, v0,
                        x10, y0, z10, u1, v0,
                        x11, y1, z11, u1, v1,
                        x01, y1, z01, u0, v1);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VortexEntity entity) {
        return SLASH_TEX;
    }
}
