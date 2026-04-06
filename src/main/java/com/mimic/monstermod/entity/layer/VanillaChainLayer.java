package com.mimic.monstermod.entity.layer;

import com.mimic.monstermod.effect.EffectRenderManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

public class VanillaChainLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public VanillaChainLayer(RenderLayerParent<T, M> parent) { super(parent); }
    @Override
    public void render(PoseStack ps, MultiBufferSource bs, int pl, T entity, float f1, float f2, float pt, float f3, float f4, float f5) {
        // マネージャーに丸投げ。これで全エフェクトが描画される
        EffectRenderManager.renderAll(entity, ps, bs, pl, pt);
    }
}
//以下のファイルのtodoのところがある場合entityに表示できなかったけどif文をなくせばlayerが表示できる　これはサーバーからクライアントに情報が伝わってないからだと思われるのでもしentityに
//todo つけたいならパケットを送ればいいんだけど if(hasEffect) を実行すると、クライアント側では「ついていない」と判定され、描画がキャンセルされてしまいます。
//

//public class VanillaChainLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
//    private final ChainModel<DummyChain> geoModel = new ChainModel<>();
//    private final DummyChain dummyChain = new DummyChain();
//    private final DummyRenderer dummyRenderer = new DummyRenderer(geoModel, dummyChain);
//
//    public VanillaChainLayer(RenderLayerParent<T, M> parent) {
//        super(parent);
//    }
//
//    @Override
//    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
//                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
//                       float netHeadYaw, float headPitch) {
//
//       todo ここBIND状態でないなら何もしない
//       todo ここ　if (!entity.hasEffect(ModEffects.BIND.get())) return;//ここがないと普通に鎖が出せる　
//
//        BakedGeoModel bakedModel = geoModel.getBakedModel(geoModel.getModelResource(dummyChain));
//        if (bakedModel == null || bakedModel.topLevelBones().isEmpty()) return;
//
//        poseStack.pushPose();
//
//        // --- 位置調整 ---
//        // JSONのoriginがY=15.5(約0.97ブロック)にあるため、その分を引いて(約-1.0f)中心に合わせる
//        float heightOffset = (entity.getBbHeight() / 2.0f) - 1.5f;
//        poseStack.translate(0, heightOffset, 0);
//
//        // --- スケール調整 ---
//        float scale = entity.getBbWidth() / 0.6f;
//        poseStack.scale(scale, scale, scale);
//
//        ResourceLocation texture = geoModel.getTextureResource(dummyChain);
//        RenderType renderType = RenderType.entityCutoutNoCull(texture);
//        VertexConsumer buffer = bufferSource.getBuffer(renderType);
//
//        dummyRenderer.reRender(
//                bakedModel, poseStack, bufferSource, dummyChain, renderType, buffer,
//                partialTick, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
//                1.0F, 1.0F, 1.0F, 1.0F);
//
//        poseStack.popPose();
//    }
//
//    private static class DummyChain implements GeoAnimatable {
//        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
//        @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {}
//        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
//        @Override public double getTick(Object entity) {
//            return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
//        }
//    }
//
//    private static class DummyRenderer implements GeoRenderer<DummyChain> {
//        private final GeoModel<DummyChain> model;
//        private final DummyChain animatable;
//        public DummyRenderer(GeoModel<DummyChain> model, DummyChain animatable) { this.model = model; this.animatable = animatable; }
//        @Override public GeoModel<DummyChain> getGeoModel() { return this.model; }
//        @Override public DummyChain getAnimatable() { return this.animatable; }
//        @Override public void fireCompileRenderLayersEvent() {}
//        @Override public boolean firePreRenderEvent(PoseStack ps, BakedGeoModel m, MultiBufferSource bs, float pt, int pl) { return true; }
//        @Override public void firePostRenderEvent(PoseStack ps, BakedGeoModel m, MultiBufferSource bs, float pt, int pl) {}
//        @Override public void updateAnimatedTextureFrame(DummyChain a) {}
//    }
//}

//todo 直したい場合

//PotionEffectSpec.java
//付与するときに visible（パーティクル）や showIcon（右上のアイコン）を true にしてみてください。
//
//Java
//public void apply(LivingEntity target) {
//    target.addEffect(new MobEffectInstance(
//            effect,
//            duration,
//            amplifier,
//            false, // ambient
//            true,  // ★ここをtrueにする（パーティクルが出る設定）
//            true   // ★ここをtrueにする（アイコンが出る設定）
//    ));
//}之で治らない可能性が高いからパケットっていう手段が必要かもね　ってこと