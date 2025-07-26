package net.mimic.monstermod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.common.entity.MimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MimicRenderer extends GeoEntityRenderer<MimicEntity> {
    public MimicRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MimicModel());
        this.shadowRadius = 0.7F;
    }

    @Override
    public void render(MimicEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}