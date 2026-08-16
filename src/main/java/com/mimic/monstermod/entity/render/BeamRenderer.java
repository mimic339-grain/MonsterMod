package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.obj.BeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * ビームの描画。
 *
 * 【作り方の考え方】
 * 太さの違う円柱を3本重ねて描く。中心ほど白く細く、外側ほど橙色で薄い。
 * これだけで「中心が焼き付くように明るく、まわりに炎のような光がまとわりつく」
 * 見た目になる(参考画像と同じ構造)。1本だとどうしてものっぺりする。
 *
 * 【UVスクロール】
 * 円柱のUVは 横(U)=円周方向 / 縦(V)=進行方向 で貼っている。
 * 毎フレーム V をずらして頂点を作り直すことで、
 * テクスチャの縦筋がビームに沿って流れていくように見える。
 * 層ごとに流れる速さを変えているので、単調な繰り返しに見えない。
 *
 * 【RenderType.eyes を使う理由】
 * エンダーマンの目などに使われている描画設定で、
 *   ・加算合成(暗い部分は何も描かれない = 光っているように見える)
 *   ・明るさ計算を無視して常に最大の明るさ(夜でも光る)
 *   ・深度バッファに書き込まない(重ねても互いに欠けない)
 * とビームに欲しい性質が全部そろっている。自前のRenderTypeを作る必要がない。
 * ただし裏面が消える設定なので、面は表裏どちらの向きでも1回ずつ描いている。
 */
public class BeamRenderer extends EntityRenderer<BeamEntity> {

    private static final ResourceLocation BEAM_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam.png");
    private static final ResourceLocation GLOW_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/effect/beam_glow.png");

    /** 円柱の分割数。増やすほど丸くなるが重くなる。8〜16で十分きれいに見える */
    private static final int SIDES = 12;
    /** テクスチャ1枚がビーム何ブロックぶんに相当するか(小さいほど筋が細かく流れる) */
    private static final float TEX_BLOCKS = 4.0F;
    /** 着弾点から伸びる光の筋の本数 */
    private static final int BURST_RAYS = 14;
    /** 着弾点から飛び散る粒の数 */
    private static final int BURST_SPARKS = 34;

    // 粒ごとのばらつき。毎フレーム同じ動きになるよう固定の種から先に作っておく
    private static final float[] SPARK_ANG = new float[BURST_SPARKS];
    private static final float[] SPARK_REACH = new float[BURST_SPARKS];
    private static final float[] SPARK_SIZE = new float[BURST_SPARKS];
    private static final float[] SPARK_PHASE = new float[BURST_SPARKS];
    private static final float[] SPARK_SPEED = new float[BURST_SPARKS];

    static {
        java.util.Random rng = new java.util.Random(20260820L);
        for (int i = 0; i < BURST_SPARKS; i++) {
            SPARK_ANG[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
            // 飛ぶ距離をばらけさせる。揃えると輪に見えてしまう
            SPARK_REACH[i] = 4.0F + rng.nextFloat() * rng.nextFloat() * 18.0F;
            SPARK_SIZE[i] = 0.5F + rng.nextFloat() * 1.3F;
            SPARK_PHASE[i] = rng.nextFloat();
            SPARK_SPEED[i] = 0.6F + rng.nextFloat() * 0.9F;
        }
    }

    public BeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BeamEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {

        float fade = entity.getFade(partialTick);
        if (fade <= 0.0F) return;

        float length = entity.getLength();
        if (length <= 0.05F) return;

        Vec3 origin = entity.getBeamOrigin(partialTick);
        Vec3 dir = entity.getBeamDirection(partialTick).normalize();

        // PoseStack はエンティティの補間座標にいるので、そこから銃口までずらす
        Vec3 entityPos = new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));

        int rgb = entity.getColor();
        float baseR = ((rgb >> 16) & 0xFF) / 255.0F;
        float baseG = ((rgb >> 8) & 0xFF) / 255.0F;
        float baseB = (rgb & 0xFF) / 255.0F;

        // アニメーションの位相はワールド時間を基準にする(全プレイヤーで揃うため)
        float time = (float) (entity.level().getGameTime() % 24000L) + partialTick;
        // 太さの揺らぎ。目立つと安っぽくなるのでごくわずかに留める
        float pulse = 1.0F + 0.015F * Mth.sin(time * 0.9F);

        float radius = entity.getRadius() * fade * pulse;

        pose.pushPose();
        pose.translate(origin.x - entityPos.x, origin.y - entityPos.y, origin.z - entityPos.z);

        // ローカルの +Z 軸がビームの向きになるように回す
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(dir.x, dir.z)));
        pose.mulPose(Axis.XP.rotation((float) -Math.asin(Mth.clamp(dir.y, -1.0, 1.0))));

        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(BEAM_TEX));

        // 外側の光 → 中間 → 芯 の順に重ねる
        drawCylinder(pose, vc, length, radius * 2.0F, radius * 2.6F,
                baseR, baseG * 0.45F, baseB * 0.15F, 0.30F * fade, time * 0.20F);
        drawCylinder(pose, vc, length, radius * 1.15F, radius * 1.5F,
                baseR, baseG, baseB, 0.80F * fade, time * 0.45F);
        drawCylinder(pose, vc, length, radius * 0.45F, radius * 0.6F,
                1.0F, 0.98F, 0.86F, 1.00F * fade, time * 0.85F);

        pose.popPose();

        // 銃口と着弾点の丸い光はカメラの方を向かせる(円柱の回転の外で描く)
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW_TEX));
        float muzzleSize = radius * 6.0F;
        drawBillboard(pose, glow, origin.subtract(entityPos), muzzleSize,
                1.0F, 0.85F, 0.55F, 0.85F * fade);

        // 着弾点。壁で途切れるだけだと当たった感じがしないので、そこで弾けるようにする。
        //
        // 【壁から手前へずらす理由】
        // この描画設定は深度テストが効いているため、壁の面ぴったりに置くと
        // 光がブロックと同じ奥行きになり、ほぼ全部が弾かれて何も見えなくなる。
        // 撃った側へ少し浮かせることで、壁の手前に確実に描かれるようにしている。
        Vec3 end = origin.add(dir.scale(length));
        Vec3 burstAt = end.subtract(dir.scale(Math.max(0.4, radius * 2.5)));
        drawImpactBurst(pose, glow, burstAt.subtract(entityPos), radius, time, baseR, baseG, baseB, fade);

        // 貫いている相手の位置でも弾けさせる。壁だけだと人に当たった手応えが無い
        drawEntityHits(entity, pose, glow, origin, end, entityPos, radius, time,
                baseR, baseG, baseB, fade);
    }

    /**
     * 射線が通っている相手の位置に、小さめの弾けを出す。
     *
     * どこに当たっているかはサーバーから送らず、クライアント側で同じ射線を引き直して求めている。
     * 見た目だけの話なので、当たり判定と厳密に一致していなくても問題はなく、
     * 通信を増やさずに済む。
     */
    private void drawEntityHits(BeamEntity beam, PoseStack pose, VertexConsumer vc,
                                Vec3 origin, Vec3 end, Vec3 entityPos, float radius,
                                float time, float r, float g, float b, float fade) {
        Entity owner = beam.getOwnerEntity();
        AABB area = new AABB(origin, end).inflate(1.0);

        for (Entity target : beam.level().getEntities(beam, area,
                e -> e != owner && e.isAlive() && e.isPickable())) {

            Optional<Vec3> hit = target.getBoundingBox().inflate(radius).clip(origin, end);
            if (hit.isEmpty()) continue;

            // 相手の体に埋まらないよう、こちらも手前へ少し浮かせる
            Vec3 back = origin.subtract(end).normalize().scale(radius * 1.5F);
            Vec3 at = hit.get().add(back);

            // 壁より控えめにする。人を貫くたびに壁と同じ大きさで光ると画面が埋まる
            drawImpactBurst(pose, vc, at.subtract(entityPos), radius * 0.7F,
                    time, r, g, b, fade * 0.85F);
        }
    }

    /**
     * 着弾点で弾ける閃光。
     *
     * 中心の白い塊と、そこから四方へ伸びる光の筋でできている。
     * 筋は長さも太さもバラバラにして、放射状に散った感じを出す。
     * 筋は常に同じ向きだと板に見えるので、カメラの方を向く面の中で
     * ゆっくり回しながら描いている。
     */
    private void drawImpactBurst(PoseStack pose, VertexConsumer vc, Vec3 offset, float radius,
                                 float time, float r, float g, float b, float fade) {
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        PoseStack.Pose last = pose.last();

        // 玉。外側に色の付いた光、内側に白い芯を重ねて「焼けている玉」にする
        float halo = radius * 11.0F * (1.0F + 0.05F * Mth.sin(time * 1.3F));
        drawFlatQuad(last, vc, halo, r, g * 0.55F, b * 0.25F, 0.55F * fade);
        drawFlatQuad(last, vc, radius * 6.5F, 1.0F, 0.9F, 0.6F, 0.85F * fade);
        drawFlatQuad(last, vc, radius * 3.2F, 1.0F, 0.99F, 0.95F, 1.0F * fade);

        // 放射状の筋
        for (int i = 0; i < BURST_RAYS; i++) {
            float ang = (float) (Math.PI * 2.0 * i / BURST_RAYS) + time * 0.02F;

            // 長さと太さを1本ずつ変える。均一だと歯車のように見えてしまう
            float variance = 0.45F + 0.55F * Mth.abs(Mth.sin(i * 12.9898F));
            float len = radius * 26.0F * variance;
            float halfWidth = radius * 1.6F * variance;

            float cos = Mth.cos(ang), sin = Mth.sin(ang);
            // 筋の向き(外向き)と、それに垂直な向き(太さ方向)
            float px = -sin * halfWidth, py = cos * halfWidth;
            float ex = cos * len, ey = sin * len;

            // 根元は白く、先へ行くほどビームの色に寄せて消えていく
            drawRay(last, vc, px, py, ex, ey, r, g, b, 0.8F * fade * variance);
        }

        // 飛び散る粒
        drawSparks(last, vc, radius, time, r, g, b, fade);

        pose.popPose();
    }

    /**
     * 着弾点から飛び散る粒。
     *
     * 外へ向かって飛びながら薄れ、消えるとまた中心から現れる。
     * これを粒ごとにずれた周期で回しているので、当たっている間ずっと弾け続けて見える。
     * 飛ぶ距離をばらけさせてあるので、外周に輪ができない。
     */
    private static void drawSparks(PoseStack.Pose pose, VertexConsumer vc, float radius,
                                   float time, float r, float g, float b, float fade) {
        for (int i = 0; i < BURST_SPARKS; i++) {
            float t = (SPARK_PHASE[i] + time * 0.045F * SPARK_SPEED[i]) % 1.0F;
            if (t < 0) t += 1.0F;

            // 出だしが速く、そこから減速する
            float travel = 1.0F - (1.0F - t) * (1.0F - t);
            float d = radius * SPARK_REACH[i] * travel;

            float x = Mth.cos(SPARK_ANG[i]) * d;
            float y = Mth.sin(SPARK_ANG[i]) * d;

            // 現れるところと消えるところで急に出入りしないようにする
            float alpha = Mth.clamp(t * 8.0F, 0.0F, 1.0F) * (1.0F - t) * fade;
            if (alpha <= 0.02F) continue;

            // 飛ぶほどビームの色に寄っていく。根元は白熱している
            float mix = travel;
            float sr = 1.0F + (r - 1.0F) * mix;
            float sg = 0.98F + (g - 0.98F) * mix;
            float sb = 0.92F + (b - 0.92F) * mix;

            float h = radius * SPARK_SIZE[i] * (1.0F - travel * 0.4F);
            VfxRenderUtil.quadBothSides(pose, vc, sr, sg, sb, alpha,
                    x - h, y - h, 0, 0, 0,
                    x + h, y - h, 0, 1, 0,
                    x + h, y + h, 0, 1, 1,
                    x - h, y + h, 0, 0, 1);
        }
    }

    /** カメラを向いた面に、中心ぞろえの四角を1枚 */
    private static void drawFlatQuad(PoseStack.Pose pose, VertexConsumer vc, float size,
                                     float r, float g, float b, float a) {
        float h = size * 0.5F;
        VfxRenderUtil.quadBothSides(pose, vc, r, g, b, a,
                -h, -h, 0, 0, 0,
                 h, -h, 0, 1, 0,
                 h,  h, 0, 1, 1,
                -h,  h, 0, 0, 1);
    }

    /** 中心から外へ伸びる1本の筋。根元が太く、先が尖った形にする */
    private static void drawRay(PoseStack.Pose pose, VertexConsumer vc,
                                float px, float py, float ex, float ey,
                                float r, float g, float b, float alpha) {
        // 根元(中心付近)の2点と、先端の1点をつぶした四角で三角形のように見せる
        VfxRenderUtil.quadBothSides(pose, vc, 1.0F, 0.98F, 0.9F, alpha,
                px, py, 0, 0.0F, 0.0F,
                -px, -py, 0, 0.0F, 1.0F,
                ex - px * 0.15F, ey - py * 0.15F, 0, 1.0F, 1.0F,
                ex + px * 0.15F, ey + py * 0.15F, 0, 1.0F, 0.0F);
    }

    /**
     * ローカル +Z 方向に伸びる円柱を1本描く。
     * startRadius→endRadius で先に向かって少しずつ太くしている(参考画像と同じく広がる形)。
     * vScroll ぶんUVを縦にずらすことで、筋が流れて見える。
     */
    private static void drawCylinder(PoseStack pose, VertexConsumer vc, float length,
                                     float startRadius, float endRadius,
                                     float r, float g, float b, float a, float vScroll) {
        PoseStack.Pose last = pose.last();

        float v0 = -vScroll;
        float v1 = v0 + length / TEX_BLOCKS;

        for (int i = 0; i < SIDES; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / SIDES);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / SIDES);

            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            float u0 = (float) i / SIDES;
            float u1 = (float) (i + 1) / SIDES;

            // 手前(z=0)は startRadius、先(z=length)は endRadius
            float x00 = c0 * startRadius, y00 = s0 * startRadius;
            float x10 = c1 * startRadius, y10 = s1 * startRadius;
            float x01 = c0 * endRadius,   y01 = s0 * endRadius;
            float x11 = c1 * endRadius,   y11 = s1 * endRadius;

            VfxRenderUtil.quadBothSides(last, vc, r, g, b, a,
                    x00, y00, 0, u0, v0,
                    x10, y10, 0, u1, v0,
                    x11, y11, length, u1, v1,
                    x01, y01, length, u0, v1);
        }
    }

    /** カメラの方を向く四角い光。銃口の閃光と着弾点に使う */
    private void drawBillboard(PoseStack pose, VertexConsumer vc, Vec3 offset, float size,
                               float r, float g, float b, float a) {
        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float h = size * 0.5F;
        VfxRenderUtil.quadBothSides(pose.last(), vc, r, g, b, a,
                -h, -h, 0, 0, 0,
                 h, -h, 0, 1, 0,
                 h,  h, 0, 1, 1,
                -h,  h, 0, 0, 1);

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BeamEntity entity) {
        return BEAM_TEX;
    }
}
