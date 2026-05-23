package com.mimic.monstermod.geo.model.monster;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.monster.YatagarasuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class YatagarasuModel extends GeoModel<YatagarasuEntity> {
    @Override
    public ResourceLocation getModelResource(YatagarasuEntity object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "geo/yatagarasu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(YatagarasuEntity object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/yatagarasu.png");
    }

    @Override
    public ResourceLocation getAnimationResource(YatagarasuEntity animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "animations/yatagarasu.animation.json");
    }
}