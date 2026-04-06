package com.mimic.monstermod.entity.layer;

import com.mimic.monstermod.effect.ModEffects;
import com.mimic.monstermod.geo.model.layer.ChainModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

public class ChainLayer implements IEffectLayer {
    private final ChainModel<GeoLayerUtil.DummyEffect> MODEL = new ChainModel<>();
    private final GeoLayerUtil.DummyEffect DUMMY = new GeoLayerUtil.DummyEffect();
    private final GeoLayerUtil.DummyRenderer RENDERER = new GeoLayerUtil.DummyRenderer(MODEL, DUMMY);

    @Override
    public boolean shouldRender(LivingEntity entity) {
        return entity.hasEffect(ModEffects.BIND.get());
    }

    @Override
    public void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTicks) {
        poseStack.pushPose();
        float heightOffset = (entity.getBbHeight() / 2.0f);
        poseStack.translate(0, heightOffset, 0);
        float scale = entity.getBbWidth() / 0.6f;
        poseStack.scale(scale, scale, scale);

        RenderType rt = RenderType.entityCutoutNoCull(MODEL.getTextureResource(DUMMY));
        RENDERER.reRender(MODEL.getBakedModel(MODEL.getModelResource(DUMMY)),
                poseStack, bufferSource, DUMMY, rt, bufferSource.getBuffer(rt),
                partialTicks, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                1, 1, 1, 1);
        poseStack.popPose();
    }
}