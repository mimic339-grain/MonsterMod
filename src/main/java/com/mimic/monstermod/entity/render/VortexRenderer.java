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
 * 【構造】外側が「風切りの弧」、内側が「うっすらした円錐」。
 *
 * 竜巻の形は円錐そのものを見せて作るのではなく、
 * 大きさも高さもバラバラな弧(風切り)をたくさん重ねることで
 * 「だいたいこの形」と分かる程度に象る。
 * こうすると見るたびに形が変わり、定型の円錐に見えなくなる。
 * 弧は一部が本体より外へはみ出すようにしてあり、
 * 外周に時々スラッシュが走るように見える。
 *
 * 内側の円錐は「中身がある」ことを示すためだけの薄い層で、
 * 弧より内側(BODY_SCALE)に置く。多角形に見えないよう分割数は多めに取っている。
 *
 * 【動き】
 *  ・弧は下から湧いて上へ昇り、上で消える(昇りながら半径も大きくなる)
 *  ・弧ごとに回る速さが違うので、全体がねじれて見える
 *  ・弧のUVを長さ方向へ流して、中を光が走るようにする
 * 位相はワールド時間基準なので全プレイヤーで同じ動きになる。
 *
 * 【明るさについて】
 * 加算合成なので、重なった枚数だけ明るさが足し算される。
 * 弧は視点によっては10枚以上重なるため、1枚あたりの濃さはかなり低くしないと
 * すぐ真っ白に飽和する(ALPHA_SLASH / ALPHA_BODY が小さいのはそのため)。
 */
public class VortexRenderer extends EntityRenderer<VortexEntity> {

    private static final ResourceLocation SLASH_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/vortex.png");
    private static final ResourceLocation BODY_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam.png");

    /** 風切りの弧の枚数 */
    private static final int SLASHES = 28;
    /** 弧1枚の分割数。少ないと折れ線に見える */
    private static final int ARC_STEPS = 18;

    /** 内側の円錐の分割数。多角形に見えないよう多めに取る */
    private static final int BODY_SIDES = 28;
    private static final int BODY_RINGS = 10;
    /** 内側の円錐は弧より内側に置く */
    private static final float BODY_SCALE = 0.72F;

    /** 円錐のすぼまり具合。1に近いほど素直な円錐、大きいほど下がくびれる */
    private static final float FLARE = 1.35F;

    // 加算合成なので1枚あたりはごく薄くする(重なって初めて濃く見える)
    private static final float ALPHA_SLASH = 0.20F;
    private static final float ALPHA_BODY = 0.05F;

    // 弧ごとのばらつき。毎フレーム同じ形になるよう固定の種から先に作っておく
    private static final float[] PHASE = new float[SLASHES];   // 開始角
    private static final float[] SPAN = new float[SLASHES];    // 弧の長さ(ラジアン)
    private static final float[] RAD = new float[SLASHES];     // 本体に対する半径の倍率
    private static final float[] WIDTH = new float[SLASHES];   // 弧の厚み(本体半径に対する割合)
    private static final float[] RISE = new float[SLASHES];    // 昇る速さ
    private static final float[] SPIN = new float[SLASHES];    // 回る速さ
    private static final float[] Y0 = new float[SLASHES];      // 初期の高さ
    private static final float[] TILT = new float[SLASHES];    // 弧に沿って昇る量(螺旋の強さ)
    private static final float[] BRIGHT = new float[SLASHES];
    private static final float[] FLOW = new float[SLASHES];    // UVが流れる速さ

    static {
        Random rng = new Random(20260817L);
        for (int i = 0; i < SLASHES; i++) {
            PHASE[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            SPAN[i] = 1.8F + rng.nextFloat() * 3.8F;           // 約100〜320度
            // 1.0を超えるものが本体からはみ出して「外に走るスラッシュ」になる
            RAD[i] = 0.82F + rng.nextFloat() * 0.55F;
            WIDTH[i] = 0.020F + rng.nextFloat() * 0.075F;
            RISE[i] = 0.004F + rng.nextFloat() * 0.010F;
            SPIN[i] = 0.10F + rng.nextFloat() * 0.22F;
            if (rng.nextBoolean()) SPIN[i] *= 0.55F;           // 遅い弧を混ぜて動きを単調にしない
            Y0[i] = rng.nextFloat();
            TILT[i] = 0.02F + rng.nextFloat() * 0.10F;
            BRIGHT[i] = 0.45F + rng.nextFloat() * 0.55F;
            FLOW[i] = 0.02F + rng.nextFloat() * 0.06F;
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

        // 内側のうっすらした円錐(中身があるように見せるだけの層)
        VertexConsumer body = buffer.getBuffer(RenderType.eyes(BODY_TEX));
        drawBody(pose, body, height, rBottom, rTop, cr, cg, cb, ALPHA_BODY * fade, time);

        // 外側の風切り
        VertexConsumer slash = buffer.getBuffer(RenderType.eyes(SLASH_TEX));
        for (int i = 0; i < SLASHES; i++) {
            drawSlash(pose, slash, i, height, rBottom, rTop, cr, cg, cb, fade, time);
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
     * 弧は下から湧いて上へ昇り、上端で消える。
     * 高さが上がるにつれて半径も自然に大きくなるので、
     * 「巻き上がりながら広がっていく」動きになる。
     */
    private static void drawSlash(PoseStack pose, VertexConsumer vc, int i,
                                  float height, float rBottom, float rTop,
                                  float cr, float cg, float cb, float fade, float time) {

        // 高さは時間で循環させる。0〜1を繰り返し、下で現れて上で消える
        float yNorm = (Y0[i] + time * RISE[i]) % 1.0F;
        if (yNorm < 0) yNorm += 1.0F;

        // 現れる/消えるところで急に出ないよう、上下で薄くする
        float env = Mth.clamp(yNorm / 0.12F, 0.0F, 1.0F)
                * Mth.clamp((1.0F - yNorm) / 0.30F, 0.0F, 1.0F);
        if (env <= 0.001F) return;

        float baseAlpha = ALPHA_SLASH * BRIGHT[i] * env * fade;
        float spin = time * SPIN[i];
        float uScroll = time * FLOW[i];

        PoseStack.Pose last = pose.last();

        float px = 0, py = 0, pz = 0, pw = 0, pu = 0;

        for (int s = 0; s <= ARC_STEPS; s++) {
            float f = (float) s / ARC_STEPS;

            // 弧に沿って少しずつ高さが上がる(完全な水平の輪にしない)
            float t = yNorm + TILT[i] * f;
            float y = Mth.clamp(t, 0.0F, 1.0F) * height;
            float r = radiusAt(t, rBottom, rTop) * RAD[i];

            float ang = PHASE[i] + spin + SPAN[i] * f;
            float x = Mth.cos(ang) * r;
            float z = Mth.sin(ang) * r;

            // 両端を細くして、先が消えていくようにする
            float taper = Mth.sin((float) Math.PI * f);
            float w = WIDTH[i] * rTop * (0.25F + 0.75F * taper);
            float u = f * SPAN[i] * 0.35F - uScroll;

            if (s > 0) {
                float a = baseAlpha * (0.15F + 0.85F * taper);
                VfxRenderUtil.quadBothSides(last, vc, cr, cg, cb, a,
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
     * 弧の隙間から向こう側が丸見えになるのを抑えるだけの層なので、かなり薄い。
     * 弧より内側(BODY_SCALE)に置き、分割数を多くして多角形に見えないようにしている。
     */
    private static void drawBody(PoseStack pose, VertexConsumer vc,
                                 float height, float rBottom, float rTop,
                                 float cr, float cg, float cb, float alpha, float time) {
        PoseStack.Pose last = pose.last();
        float spin = time * 0.04F;
        float vScroll = time * 0.04F;

        for (int ring = 0; ring < BODY_RINGS; ring++) {
            float t0 = (float) ring / BODY_RINGS;
            float t1 = (float) (ring + 1) / BODY_RINGS;
            float y0 = t0 * height, y1 = t1 * height;
            float r0 = radiusAt(t0, rBottom, rTop) * BODY_SCALE;
            float r1 = radiusAt(t1, rBottom, rTop) * BODY_SCALE;

            // 上端は空へ、下端は地面へ溶けるように薄くする
            float a = alpha * Mth.sin((float) Math.PI * Mth.clamp((t0 + t1) * 0.5F, 0.0F, 1.0F));
            if (a <= 0.001F) continue;

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
        return SLASH_TEX;
    }
}
