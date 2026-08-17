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
 * 【絵を貼るのをやめた理由】
 * 用意した何枚かの絵を順番に切り替える方式では、
 *   ・絵が平らなので、どの角度から見ても板にしか見えない
 *   ・枚数ぶんの形しか出ないので、並べると同じ動きが揃って目立つ
 *   ・輪郭が絵のままなので丸みが出ない
 * という問題があった。
 *
 * 【丸い光の粒を重ねて作る】
 * 中心が明るく外へ向かって消える丸を、たくさん重ねて炎の形にしている。
 * 丸が重なった部分は自然に太く明るくなるので、輪郭がぷっくりと丸くなる。
 * 粒はそれぞれ独立に揺れるので、同じ形が繰り返されることもない。
 *
 * 【構成】
 *  芯   … 中心の白く明るい塊
 *  胴   … 芯を包む色付きの玉。鬼火の「丸さ」はここで決まる
 *  舌   … 胴から上へ伸びる炎。左右に揺れながら細く薄くなっていく
 *  飛沫 … 舌の先から離れて昇っていく小さな粒
 *
 * 加算合成で描いているので、重なるほど明るくなり芯が白く飛ぶ。
 * この描画設定は裏面を捨てるため、面は表裏1回ずつ描いている。
 */
public class OnibiRenderer extends EntityRenderer<OnibiEntity> {

    /** 中心が明るく外へ向かって消える丸 */
    private static final ResourceLocation TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");

    /** 全体の大きさ(ブロック) */
    private static final float SCALE = 0.55F;

    private static final int CORE = 5;      // 芯
    private static final int BODY = 18;     // 胴
    private static final int TONGUE = 26;   // 舌
    private static final int DROPS = 10;    // 飛沫

    // 色。外へ向かうほど青が濃くなり、芯へ向かうほど白くなる
    private static final float[] COLOR_OUT  = { 0.20F, 0.45F, 1.00F };
    private static final float[] COLOR_MID  = { 0.45F, 0.75F, 1.00F };
    private static final float[] COLOR_CORE = { 0.90F, 0.97F, 1.00F };

    // --- 粒ごとのばらつき。毎フレーム同じ配置になるよう固定の種から先に作っておく ---
    private static final float[] BODY_YAW = new float[BODY];
    private static final float[] BODY_PITCH = new float[BODY];
    private static final float[] BODY_DIST = new float[BODY];
    private static final float[] BODY_SIZE = new float[BODY];
    private static final float[] BODY_PHASE = new float[BODY];

    private static final float[] CORE_X = new float[CORE];
    private static final float[] CORE_Y = new float[CORE];
    private static final float[] CORE_SIZE = new float[CORE];

    private static final float[] TONGUE_T = new float[TONGUE];      // 下から上への位置
    private static final float[] TONGUE_SIDE = new float[TONGUE];   // 横へのずれ
    private static final float[] TONGUE_SIZE = new float[TONGUE];
    private static final float[] TONGUE_PHASE = new float[TONGUE];
    private static final float[] TONGUE_SWAY = new float[TONGUE];   // 揺れの大きさ

    private static final float[] DROP_SIDE = new float[DROPS];
    private static final float[] DROP_SIZE = new float[DROPS];
    private static final float[] DROP_PHASE = new float[DROPS];
    private static final float[] DROP_SPEED = new float[DROPS];

    static {
        Random rng = new Random(20260821L);

        for (int i = 0; i < BODY; i++) {
            BODY_YAW[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            BODY_PITCH[i] = (float) Math.asin(rng.nextFloat() * 2.0F - 1.0F);
            // 中心寄りに集めると、外側がぼやけず丸い塊に見える
            BODY_DIST[i] = 0.10F + rng.nextFloat() * rng.nextFloat() * 0.34F;
            BODY_SIZE[i] = 0.40F + rng.nextFloat() * 0.32F;
            BODY_PHASE[i] = rng.nextFloat() * 10.0F;
        }
        for (int i = 0; i < CORE; i++) {
            CORE_X[i] = (rng.nextFloat() - 0.5F) * 0.14F;
            CORE_Y[i] = (rng.nextFloat() - 0.5F) * 0.14F;
            CORE_SIZE[i] = 0.26F + rng.nextFloat() * 0.20F;
        }
        for (int i = 0; i < TONGUE; i++) {
            // 下ほど密になるようにして、根元が太く先が細い炎の形にする
            TONGUE_T[i] = rng.nextFloat() * rng.nextFloat();
            TONGUE_SIDE[i] = (rng.nextFloat() - 0.5F) * 0.5F;
            TONGUE_SIZE[i] = 0.26F + rng.nextFloat() * 0.26F;
            TONGUE_PHASE[i] = rng.nextFloat() * 10.0F;
            TONGUE_SWAY[i] = 0.10F + rng.nextFloat() * 0.22F;
        }
        for (int i = 0; i < DROPS; i++) {
            DROP_SIDE[i] = (rng.nextFloat() - 0.5F) * 0.7F;
            DROP_SIZE[i] = 0.10F + rng.nextFloat() * 0.14F;
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

        // カメラの向きは1回だけ求めて使い回す。粒ごとに行列を積むと重い
        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        Vector3f right = new Vector3f(1, 0, 0).rotate(cam);
        Vector3f up = new Vector3f(0, 1, 0).rotate(cam);

        pose.pushPose();
        // 見た目の中心を少し持ち上げる。当たり判定の足元に合わせると埋まって見える
        pose.translate(0.0, 0.25, 0.0);
        PoseStack.Pose p = pose.last();

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(TEX));

        // 外→内の順に重ねる。加算合成なので、重なった中心ほど白く飛ぶ
        drawBody(p, vc, time, right, up);
        drawTongue(p, vc, time, right, up);
        drawCore(p, vc, time, right, up);
        drawDrops(p, vc, time, right, up);

        pose.popPose();
    }

    /**
     * 胴。丸く並べた粒で、鬼火の「玉」の部分を作る。
     * 粒ごとにゆっくり伸び縮みさせているので、輪郭がゆらゆら動いて見える。
     */
    private static void drawBody(PoseStack.Pose pose, VertexConsumer vc, float time,
                                 Vector3f right, Vector3f up) {
        for (int i = 0; i < BODY; i++) {
            float breathe = 1.0F + 0.18F * Mth.sin(time * 0.22F + BODY_PHASE[i]);
            float d = BODY_DIST[i] * SCALE * breathe;

            float cy = Mth.cos(BODY_PITCH[i]);
            float x = Mth.cos(BODY_YAW[i]) * cy * d;
            float y = Mth.sin(BODY_PITCH[i]) * d;
            float z = Mth.sin(BODY_YAW[i]) * cy * d;

            // 外側の粒ほど青く暗く、内側ほど明るい色へ寄せる
            float t = Mth.clamp(BODY_DIST[i] / 0.44F, 0.0F, 1.0F);
            float r = Mth.lerp(t, COLOR_MID[0], COLOR_OUT[0]);
            float g = Mth.lerp(t, COLOR_MID[1], COLOR_OUT[1]);
            float b = Mth.lerp(t, COLOR_MID[2], COLOR_OUT[2]);

            blob(pose, vc, x, y, z, BODY_SIZE[i] * SCALE * breathe, right, up,
                    r, g, b, 0.42F);
        }
    }

    /** 芯。ここだけほぼ白くして、玉の中心が焼けているように見せる */
    private static void drawCore(PoseStack.Pose pose, VertexConsumer vc, float time,
                                 Vector3f right, Vector3f up) {
        float breathe = 1.0F + 0.12F * Mth.sin(time * 0.35F);
        for (int i = 0; i < CORE; i++) {
            blob(pose, vc, CORE_X[i] * SCALE, CORE_Y[i] * SCALE, 0.0F,
                    CORE_SIZE[i] * SCALE * breathe, right, up,
                    COLOR_CORE[0], COLOR_CORE[1], COLOR_CORE[2], 0.85F);
        }
    }

    /**
     * 舌。胴から上へ伸びる炎。
     *
     * 上へ行くほど細く薄くなり、横揺れは大きくなる。
     * 根元は胴と重なっているので、玉から炎が生えているように繋がって見える。
     */
    private static void drawTongue(PoseStack.Pose pose, VertexConsumer vc, float time,
                                   Vector3f right, Vector3f up) {
        for (int i = 0; i < TONGUE; i++) {
            float t = TONGUE_T[i];

            // 上ほど大きく揺れる。根元が揺れると玉から外れて見える
            float sway = Mth.sin(time * 0.30F + TONGUE_PHASE[i] + t * 3.0F)
                    * TONGUE_SWAY[i] * t;

            float x = (TONGUE_SIDE[i] * (1.0F - t * 0.55F) + sway) * SCALE;
            float y = (0.18F + t * 1.15F) * SCALE;
            float z = TONGUE_SIDE[i] * 0.5F * (1.0F - t) * SCALE;

            float size = TONGUE_SIZE[i] * (1.0F - t * 0.70F) * SCALE;
            float alpha = 0.50F * (1.0F - t * 0.85F);

            // 根元は明るく、先へ行くほど青くなって消えていく
            float r = Mth.lerp(t, COLOR_MID[0], COLOR_OUT[0]);
            float g = Mth.lerp(t, COLOR_MID[1], COLOR_OUT[1]);
            float b = Mth.lerp(t, COLOR_MID[2], COLOR_OUT[2]);

            blob(pose, vc, x, y, z, size, right, up, r, g, b, alpha);
        }
    }

    /** 飛沫。舌の先から離れて昇り、消えるとまた下から現れる */
    private static void drawDrops(PoseStack.Pose pose, VertexConsumer vc, float time,
                                  Vector3f right, Vector3f up) {
        for (int i = 0; i < DROPS; i++) {
            float t = (DROP_PHASE[i] + time * 0.020F * DROP_SPEED[i]) % 1.0F;
            if (t < 0) t += 1.0F;

            float x = (DROP_SIDE[i] + Mth.sin(time * 0.4F + i) * 0.12F * t) * SCALE;
            float y = (0.55F + t * 1.30F) * SCALE;

            // 現れるところと消えるところで急に出入りしないようにする
            float alpha = Mth.clamp(t * 5.0F, 0.0F, 1.0F) * (1.0F - t) * 0.8F;

            blob(pose, vc, x, y, 0.0F, DROP_SIZE[i] * (1.0F - t * 0.5F) * SCALE, right, up,
                    COLOR_MID[0], COLOR_MID[1], COLOR_MID[2], alpha);
        }
    }

    /**
     * カメラの方を向く丸い光を1つ。
     *
     * あらかじめ求めたカメラの右方向・上方向から四隅を作っているので、
     * 粒ごとに行列を積む必要がない。
     * 裏面を捨てる描画設定なので、面は表裏1回ずつ描いている。
     */
    private static void blob(PoseStack.Pose pose, VertexConsumer vc,
                             float x, float y, float z, float size,
                             Vector3f right, Vector3f up,
                             float r, float g, float b, float a) {
        if (a <= 0.01F || size <= 0.0F) return;

        float h = size * 0.5F;
        float rx = right.x * h, ry = right.y * h, rz = right.z * h;
        float ux = up.x * h, uy = up.y * h, uz = up.z * h;

        VfxRenderUtil.quadBothSides(pose, vc, r, g, b, a,
                x - rx - ux, y - ry - uy, z - rz - uz, 0.0F, 0.0F,
                x + rx - ux, y + ry - uy, z + rz - uz, 1.0F, 0.0F,
                x + rx + ux, y + ry + uy, z + rz + uz, 1.0F, 1.0F,
                x - rx + ux, y - ry + uy, z - rz + uz, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(OnibiEntity entity) {
        return TEX;
    }
}
