package com.mimic.monster.entity.monster.client;

import com.mimic.monster.entity.monster.client.MimicModel;
import com.mimic.monster.entity.monster.Mimic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class MimicRenderer extends GeoEntityRenderer<Mimic> {

    public MimicRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MimicModel());
        this.shadowRadius = 0.7f; // 影の大きさなど調整
    }
}