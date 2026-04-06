package com.mimic.monstermod.geo.model.item;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.item.weapon.SimpleSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SimpleSwordModel extends GeoModel<SimpleSwordItem> {

    @Override
    public ResourceLocation getModelResource(SimpleSwordItem object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "geo/sword_simple.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SimpleSwordItem object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/item/sword_simple.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SimpleSwordItem animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "animations/item.animation.json");
    }
}
