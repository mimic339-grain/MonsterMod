package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.obj.LightningBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 電撃の描画。
 *
 * 【形の作り方】
 * 始点と終点を結ぶ線を用意し、その中点を横へずらす。
 * できた2本それぞれについて同じことを繰り返すと、
 * 大きな折れの中に小さな折れが入った稲妻らしい形になる(ずらす量は毎回半分にする)。
 * 途中の折れ点からは枝を伸ばし、その枝にも同じ処理をかけている。
 *
 * 【形を毎フレーム作り直す理由】
 * 稲妻は一瞬ごとに形が変わる。固定した形を出し続けると棒が置いてあるように見える。
 * ただし完全に毎フレーム変えるとブレすぎるので、
 * 数tickごとに作り直して、その間は同じ形を保っている。
 * 種と時間から形を決めているので、誰の画面でも同じ形になる。
 *
 * 【線を板で描く方法】
 * 各区間について「区間の向き」と「カメラへの向き」の外積を取ると、
 * 画面上で線に対して垂直な向きが出る。そこへ太さぶん広げれば、
 * どこから見ても同じ太さに見える帯になる。
 *
 * 使う描画設定は {@code RenderType.eyes}(加算合成・常時最大の明るさ)。
 * この設定は裏面を捨てるため、面は必ず表裏1回ずつ描くこと。
 */
public class LightningBoltRenderer extends EntityRenderer<LightningBoltEntity> {

    /** 光の粒の絵。中心が明るく外へ向かって消える */
    private static final ResourceLocation TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");

    /** 中点をずらす回数。5回で本線が32区間になる */
    private static final int SUBDIVISIONS = 5;
    /** 最初にずらす量(全長に対する割合)。大きいほど暴れる */
    private static final float JAGGED = 0.16F;
    /** 枝が生える確率と、その長さ(本線に対する割合) */
    private static final float BRANCH_CHANCE = 0.28F;
    private static final float BRANCH_LENGTH = 0.35F;

    public LightningBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LightningBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {

        float intensity = entity.getIntensity(partialTick);
        if (intensity <= 0.01F) return;

        Vec3 end = entity.getRelativeEnd();
        if (end.lengthSqr() < 0.01) return;

        int rgb = entity.getColor();
        float cr = ((rgb >> 16) & 0xFF) / 255.0F;
        float cg = ((rgb >> 8) & 0xFF) / 255.0F;
        float cb = (rgb & 0xFF) / 255.0F;

        // 数tickごとに形を作り直す。種と時間から決めているので全員の画面で同じ形になる
        int step = entity.tickCount / LightningBoltEntity.FLICKER_TICKS;
        List<Vec3[]> strands = buildBolt(entity.getSeed() + step * 7919, end);

        // カメラの位置をこのエンティティから見た相対座標にしておく。
        // 区間ごとに「画面上で線に垂直な向き」を出すのに使う
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();
        Vec3 entityPos = new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        Vec3 camLocal = cam.subtract(entityPos);

        PoseStack.Pose last = pose.last();
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(TEX));

        float w = entity.getThickness() * intensity;

        // 外側の光 → 中間 → 芯 の順に重ねる。中心ほど白く細い
        for (Vec3[] strand : strands) {
            float scale = strand == strands.get(0) ? 1.0F : 0.55F; // 枝は細く

            drawStrand(last, vc, strand, camLocal, w * 4.5F * scale,
                    cr * 0.5F, cg * 0.7F, cb, 0.22F * intensity);
            drawStrand(last, vc, strand, camLocal, w * 2.0F * scale,
                    cr, cg, cb, 0.55F * intensity);
            drawStrand(last, vc, strand, camLocal, w * 0.8F * scale,
                    1.0F, 1.0F, 1.0F, 0.95F * intensity);
        }
    }

    // ---------------- 形を作る ----------------

    /**
     * 稲妻の経路を作る。戻り値の先頭が本線で、以降が枝。
     *
     * 中点をずらして細かくしていく方法なので、
     * 大きなうねりの中に小さなギザギザが入った自然な形になる。
     */
    private static List<Vec3[]> buildBolt(int seed, Vec3 end) {
        Random rng = new Random(seed);
        List<Vec3[]> strands = new ArrayList<>();

        List<Vec3> main = subdivide(rng, Vec3.ZERO, end, end.length() * JAGGED);
        strands.add(main.toArray(new Vec3[0]));

        // 枝。本線の折れ点から短い稲妻を伸ばす
        for (int i = 2; i < main.size() - 2; i++) {
            if (rng.nextFloat() > BRANCH_CHANCE) continue;

            Vec3 from = main.get(i);
            Vec3 along = main.get(i + 1).subtract(main.get(i - 1)).normalize();
            // 進行方向から斜めに逸れる向き
            Vec3 off = randomPerpendicular(rng, along)
                    .scale(0.7).add(along.scale(0.6)).normalize();

            double len = end.length() * BRANCH_LENGTH * (0.4 + rng.nextDouble() * 0.6);
            Vec3 to = from.add(off.scale(len));

            List<Vec3> branch = subdivide(rng, from, to, len * JAGGED * 1.4);
            strands.add(branch.toArray(new Vec3[0]));
        }
        return strands;
    }

    /** 中点をずらす処理を繰り返して、1本のジグザグな線を作る */
    private static List<Vec3> subdivide(Random rng, Vec3 from, Vec3 to, double amplitude) {
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        points.add(to);

        double amp = amplitude;
        for (int pass = 0; pass < SUBDIVISIONS; pass++) {
            List<Vec3> next = new ArrayList<>(points.size() * 2);

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                Vec3 mid = a.add(b).scale(0.5);

                Vec3 dir = b.subtract(a);
                if (dir.lengthSqr() > 1.0E-6) {
                    Vec3 off = randomPerpendicular(rng, dir.normalize())
                            .scale(amp * (rng.nextDouble() * 2.0 - 1.0));
                    mid = mid.add(off);
                }
                next.add(a);
                next.add(mid);
            }
            next.add(points.get(points.size() - 1));

            points = next;
            amp *= 0.5; // ずらす量を半分にして、細かい折れにしていく
        }
        return points;
    }

    /** 与えた向きに垂直な、ランダムな向きを1つ返す */
    private static Vec3 randomPerpendicular(Random rng, Vec3 dir) {
        // dir と平行になりにくい適当な軸を選んでから外積を取る
        Vec3 axis = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 a = dir.cross(axis).normalize();
        Vec3 b = dir.cross(a).normalize();

        double t = rng.nextDouble() * Math.PI * 2.0;
        return a.scale(Math.cos(t)).add(b.scale(Math.sin(t)));
    }

    // ---------------- 描く ----------------

    /**
     * 折れ線を、太さのある帯として描く。
     *
     * 区間の向きとカメラへの向きの外積が、画面上で線に垂直な向きになる。
     * そこへ太さぶん広げるので、どの角度から見ても同じ太さに見える。
     */
    private static void drawStrand(PoseStack.Pose pose, VertexConsumer vc, Vec3[] points,
                                   Vec3 camLocal, float width,
                                   float r, float g, float b, float a) {
        if (a <= 0.01F) return;

        for (int i = 0; i < points.length - 1; i++) {
            Vec3 p0 = points[i];
            Vec3 p1 = points[i + 1];

            Vec3 seg = p1.subtract(p0);
            if (seg.lengthSqr() < 1.0E-8) continue;

            Vec3 toCam = camLocal.subtract(p0.add(p1).scale(0.5));
            Vec3 perp = seg.cross(toCam);
            if (perp.lengthSqr() < 1.0E-8) continue;
            perp = perp.normalize().scale(width * 0.5);

            // 裏面を捨てる描画設定なので、表裏どちらの向きでも1回ずつ描く
            VfxRenderUtil.quadBothSides(pose, vc, r, g, b, a,
                    (float) (p0.x - perp.x), (float) (p0.y - perp.y), (float) (p0.z - perp.z), 0.0F, 0.0F,
                    (float) (p1.x - perp.x), (float) (p1.y - perp.y), (float) (p1.z - perp.z), 0.0F, 1.0F,
                    (float) (p1.x + perp.x), (float) (p1.y + perp.y), (float) (p1.z + perp.z), 1.0F, 1.0F,
                    (float) (p0.x + perp.x), (float) (p0.y + perp.y), (float) (p0.z + perp.z), 1.0F, 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(LightningBoltEntity entity) {
        return TEX;
    }
}
