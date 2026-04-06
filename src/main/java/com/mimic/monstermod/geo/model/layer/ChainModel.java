package com.mimic.monstermod.geo.model.layer;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class ChainModel<T extends GeoAnimatable> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "geo/chain.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/chain.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "animations/empty.animation.json");
    }
}