package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.entity.obj.TornadoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TornadoRenderer extends EntityRenderer<TornadoEntity> {
    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TornadoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TornadoEntity entity) {
        // 本体は見えないのでダミーのパスを返す
        return null;
    }
}