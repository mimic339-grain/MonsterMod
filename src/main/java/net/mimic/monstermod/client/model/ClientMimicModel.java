package net.mimic.monstermod.client.model;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.client.ClientMimicEntity;

public class ClientMimicModel extends GeoModel<ClientMimicEntity> {

    @Override
    public ResourceLocation getModelResource(ClientMimicEntity object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "geo/mimic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ClientMimicEntity object) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ClientMimicEntity animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "animations/mimic_animation.json");
    }
}
