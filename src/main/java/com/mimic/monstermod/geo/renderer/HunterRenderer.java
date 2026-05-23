package com.mimic.monstermod.geo.renderer;

import com.mimic.monstermod.entity.HunterEntity;
import com.mimic.monstermod.geo.model.HunterModel; // 先ほど作ったModel
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HunterRenderer extends GeoEntityRenderer<HunterEntity> {

    public HunterRenderer(EntityRendererProvider.Context context) {
        super(context, new HunterModel());
    }

    @Override
    public void render(HunterEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}