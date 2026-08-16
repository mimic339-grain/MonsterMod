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
import net.minecraft.world.phys.Vec3;

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

        Vec3 hitPos = origin.add(dir.scale(length)).subtract(entityPos);
        drawBillboard(pose, glow, hitPos, radius * 4.0F * (1.0F + 0.03F * Mth.sin(time * 1.7F)),
                baseR, baseG, baseB, 0.7F * fade);
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
        VfxRenderUtil.quad(pose.last(), vc, r, g, b, a,
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
