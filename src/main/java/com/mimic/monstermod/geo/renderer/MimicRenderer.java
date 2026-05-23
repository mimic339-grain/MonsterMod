package com.mimic.monstermod.geo.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mimic.monstermod.geo.model.monster.MimicModel;
import com.mimic.monstermod.entity.monster.MimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MimicRenderer extends GeoEntityRenderer<MimicEntity> {

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new MimicModel());
    }

    @Override
    public void render(MimicEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
