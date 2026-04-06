package com.mimic.monstermod.entity.layer;

import net.minecraft.client.Minecraft;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeoLayerUtil {
    // デバフなどの外部モデルを GeckoLib で描画するための汎用ダミー
    public static class DummyEffect implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
        @Override public double getTick(Object entity) {
            return Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        }
    }

    public static class DummyRenderer implements GeoRenderer<DummyEffect> {
        private final GeoModel<DummyEffect> model;
        private final DummyEffect animatable;
        public DummyRenderer(GeoModel<DummyEffect> model, DummyEffect animatable) { this.model = model; this.animatable = animatable; }
        @Override public GeoModel<DummyEffect> getGeoModel() { return this.model; }
        @Override public DummyEffect getAnimatable() { return this.animatable; }
        @Override public void fireCompileRenderLayersEvent() {}
        @Override public boolean firePreRenderEvent(com.mojang.blaze3d.vertex.PoseStack ps, BakedGeoModel m, net.minecraft.client.renderer.MultiBufferSource bs, float pt, int pl) { return true; }
        @Override public void firePostRenderEvent(com.mojang.blaze3d.vertex.PoseStack ps, BakedGeoModel m, net.minecraft.client.renderer.MultiBufferSource bs, float pt, int pl) {}
        @Override public void updateAnimatedTextureFrame(DummyEffect a) {}
    }
}