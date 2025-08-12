package com.mimic.monster.entity.monster.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MimicModel<T extends LivingEntity & GeoEntity> extends DefaultedEntityGeoModel<T> {

    public MimicModel() {
        //MODID+Monster名でMIMICに関連したものを自動で探してそれを登録する感じ
        super(new ResourceLocation("monstermod", "mimic"));
    }

    @Override
    public ResourceLocation getModelResource(T object) {
        return new ResourceLocation("monstermod", "geo/mimic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T object) {
        return new ResourceLocation("monstermod", "textures/entity/mimic.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return new ResourceLocation("monstermod", "animations/mimic.animation.json");
    }
}
