package net.mimic.monstermod.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import net.mimic.monstermod.common.entity.MimicEntity; // この行を確認/修正

public class MimicModel extends GeoModel<MimicEntity> {

    @Override
    public ResourceLocation getModelResource(MimicEntity animatable) {
        return MimicEntity.MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(MimicEntity animatable) {
        return MimicEntity.TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(MimicEntity animatable) {
        return MimicEntity.ANIMATION_RESOURCE;
    }
}