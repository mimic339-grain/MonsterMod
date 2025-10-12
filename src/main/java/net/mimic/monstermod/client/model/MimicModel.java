package net.mimic.monstermod.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.monster.MimicEntity;

public class MimicModel extends GeoModel<MimicEntity> {
    @Override
    public ResourceLocation getModelResource(MimicEntity object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "geo/mimic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MimicEntity object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MimicEntity animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "animations/mimic_animation.json");
    }
}