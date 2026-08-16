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
 * 【作り方の考え方】
 * 参考画像の竜巻は「太さの違う光の帯が、円錐に何重にも巻き付いている」形をしている。
 * これをBlockbenchでモデルとして作ると、帯1枚ごとにパーツが要るうえ
 * 回転させるとすぐ破綻するので、ここではコードで組み立てている。
 *
 * 1本の帯は、円錐の表面を螺旋状に登っていく細長い板。
 *   ・高さが上がるほど半径が大きくなる(下がすぼまった円錐)
 *   ・帯の端は細く/薄くして、煙のように消えるようにする
 *   ・帯ごとに巻き数・太さ・高さの範囲・回る速さを変える
 * これを7本重ね、さらに内側にうっすらとした円錐の本体を入れると、
 * 「芯があって、そのまわりを光の筋が回っている」ように見える。
 *
 * 【動かし方】
 *  ・全体をY軸まわりに回す → 竜巻が回転して見える
 *  ・帯のUVを長さ方向へ流す → 帯の中を光が走って見える
 * 位相はワールド時間を基準にしているので、全プレイヤーで同じ動きになる。
 *
 * 描画設定と面の出し方は {@link VfxRenderUtil} を参照。
 */
public class VortexRenderer extends EntityRenderer<VortexEntity> {

    private static final ResourceLocation RIBBON_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/vortex.png");
    private static final ResourceLocation BODY_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam.png");

    /** 帯の本数。増やすほど密になるが重くなる */
    private static final int RIBBONS = 7;
    /** 帯1本をいくつに分割するか。少ないとカクカクした螺旋になる */
    private static final int STEPS = 22;

    /** 内側のうっすらした円錐の分割数 */
    private static final int BODY_SIDES = 12;
    private static final int BODY_RINGS = 6;

    /** 円錐のすぼまり具合。1より大きいほど下が細く、上で一気に開く */
    private static final float FLARE = 1.7F;

    // 帯ごとの見た目のばらつき。毎フレーム同じ形になるよう固定の種から先に作っておく
    private static final float[] PHASE = new float[RIBBONS];
    private static final float[] TURNS = new float[RIBBONS];
    private static final float[] Y_START = new float[RIBBONS];
    private static final float[] Y_END = new float[RIBBONS];
    private static final float[] WIDTH = new float[RIBBONS];
    private static final float[] SPIN = new float[RIBBONS];
    private static final float[] FLOW = new float[RIBBONS];
    private static final float[] BRIGHT = new float[RIBBONS];

    static {
        Random rng = new Random(20260816L);
        for (int i = 0; i < RIBBONS; i++) {
            PHASE[i] = (float) (Math.PI * 2.0 * i / RIBBONS) + rng.nextFloat() * 0.8F;
            TURNS[i] = 1.4F + rng.nextFloat() * 1.6F;          // 巻き数
            Y_START[i] = rng.nextFloat() * 0.35F;              // 高さの範囲(0〜1)
            Y_END[i] = 0.55F + rng.nextFloat() * 0.45F;
            WIDTH[i] = 0.10F + rng.nextFloat() * 0.16F;        // 帯の太さ(ブロック)
            SPIN[i] = 1.5F + rng.nextFloat() * 1.3F;           // 回る速さ
            FLOW[i] = 0.8F + rng.nextFloat() * 1.2F;           // 帯の中を光が走る速さ
            BRIGHT[i] = 0.55F + rng.nextFloat() * 0.45F;
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

        // うっすらした円錐の本体(帯だけだと中がスカスカに見えるため)
        VertexConsumer body = buffer.getBuffer(RenderType.eyes(BODY_TEX));
        drawBody(pose, body, height, rBottom, rTop, cr, cg, cb, 0.16F * fade, time);

        // 光の帯
        VertexConsumer ribbon = buffer.getBuffer(RenderType.eyes(RIBBON_TEX));
        for (int i = 0; i < RIBBONS; i++) {
            drawRibbon(pose, ribbon, i, height, rBottom, rTop, cr, cg, cb, fade, time);
        }

        pose.popPose();
    }

    /** 高さの割合(0〜1)から、その高さでの半径を求める */
    private static float radiusAt(float t, float rBottom, float rTop) {
        return rBottom + (rTop - rBottom) * (float) Math.pow(t, FLARE);
    }

    /**
     * 光の帯を1本描く。
     * 円錐の表面を螺旋状に登りながら、上下に厚みを持った細長い板を貼っていく。
     */
    private static void drawRibbon(PoseStack pose, VertexConsumer vc, int index,
                                   float height, float rBottom, float rTop,
                                   float cr, float cg, float cb, float fade, float time) {
        PoseStack.Pose last = pose.last();

        float yStart = Y_START[index];
        float yEnd = Y_END[index];
        float spin = time * SPIN[index] * 0.12F;
        float uScroll = time * FLOW[index] * 0.06F;
        float alpha = BRIGHT[index] * fade;

        float px = 0, py = 0, pz = 0, pw = 0, pu = 0;

        for (int s = 0; s <= STEPS; s++) {
            float f = (float) s / STEPS;              // 帯に沿った位置(0〜1)
            float t = yStart + (yEnd - yStart) * f;   // 高さの割合
            float y = t * height;
            float r = radiusAt(t, rBottom, rTop);

            float ang = PHASE[index] + TURNS[index] * (float) (Math.PI * 2.0) * f + spin;
            float x = Mth.cos(ang) * r;
            float z = Mth.sin(ang) * r;

            // 両端を細くして、煙のように消えていくようにする
            float taper = Mth.sin((float) Math.PI * f);
            float w = WIDTH[index] * (0.35F + 0.65F * taper);
            float u = f * TURNS[index] * 1.2F - uScroll;

            if (s > 0) {
                // 帯の色。上へ行くほどわずかに白を混ぜて明るくする
                float mix = 0.25F * t;
                float rr = cr + (1.0F - cr) * mix;
                float gg = cg + (1.0F - cg) * mix;
                float bb = cb + (1.0F - cb) * mix;
                float a = alpha * (0.30F + 0.70F * taper);

                VfxRenderUtil.quadBothSides(last, vc, rr, gg, bb, a,
                        px, py - pw, pz, pu, 0.0F,
                        x,  y  - w,  z,  u,  0.0F,
                        x,  y  + w,  z,  u,  1.0F,
                        px, py + pw, pz, pu, 1.0F);
            }

            px = x; py = y; pz = z; pw = w; pu = u;
        }
    }

    /**
     * 内側のうっすらした円錐。
     * 帯の隙間から向こう側が透けて見えるのを抑え、「中身がある」ように見せる。
     */
    private static void drawBody(PoseStack pose, VertexConsumer vc,
                                 float height, float rBottom, float rTop,
                                 float cr, float cg, float cb, float alpha, float time) {
        PoseStack.Pose last = pose.last();
        float spin = time * 0.05F;
        float vScroll = time * 0.05F;

        for (int ring = 0; ring < BODY_RINGS; ring++) {
            float t0 = (float) ring / BODY_RINGS;
            float t1 = (float) (ring + 1) / BODY_RINGS;
            float y0 = t0 * height, y1 = t1 * height;
            float r0 = radiusAt(t0, rBottom, rTop), r1 = radiusAt(t1, rBottom, rTop);

            // 上端は空へ溶けるように、下端は地面に馴染むように薄くする
            float a0 = alpha * Mth.sin((float) Math.PI * Math.min(1.0F, t0 * 1.15F));
            float a1 = alpha * Mth.sin((float) Math.PI * Math.min(1.0F, t1 * 1.15F));

            for (int i = 0; i < BODY_SIDES; i++) {
                float ang0 = (float) (Math.PI * 2.0 * i / BODY_SIDES) + spin;
                float ang1 = (float) (Math.PI * 2.0 * (i + 1) / BODY_SIDES) + spin;

                float x00 = Mth.cos(ang0) * r0, z00 = Mth.sin(ang0) * r0;
                float x10 = Mth.cos(ang1) * r0, z10 = Mth.sin(ang1) * r0;
                float x01 = Mth.cos(ang0) * r1, z01 = Mth.sin(ang0) * r1;
                float x11 = Mth.cos(ang1) * r1, z11 = Mth.sin(ang1) * r1;

                float u0 = (float) i / BODY_SIDES;
                float u1 = (float) (i + 1) / BODY_SIDES;
                float v0 = t0 * 2.0F - vScroll;
                float v1 = t1 * 2.0F - vScroll;

                // 上下で濃さが違うので、2枚に分けず1枚の平均で描く(段差はほぼ見えない)
                float a = (a0 + a1) * 0.5F;
                VfxRenderUtil.quadBothSides(last, vc, cr, cg, cb, a,
                        x00, y0, z00, u0, v0,
                        x10, y0, z10, u1, v0,
                        x11, y1, z11, u1, v1,
                        x01, y1, z01, u0, v1);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VortexEntity entity) {
        return RIBBON_TEX;
    }
}
