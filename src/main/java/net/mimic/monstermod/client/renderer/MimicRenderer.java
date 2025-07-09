package net.mimic.monstermod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.client.model.MimicModel;
import net.mimic.monstermod.entity.custom.MimicEntity;

public class MimicRenderer extends GeoEntityRenderer<MimicEntity> {
    public MimicRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MimicModel());
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }
}
