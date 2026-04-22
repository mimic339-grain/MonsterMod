package com.mimic.monstermod.geo.model;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.HunterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HunterModel extends GeoModel<HunterEntity> {

    @Override
    public ResourceLocation getModelResource(HunterEntity object) {
        // スリムモデルかどうかで切り替え
        return object.isSlim()
                ? new ResourceLocation(MonsterMod.MOD_ID, "geo/player_slim.geo.json")
                : new ResourceLocation(MonsterMod.MOD_ID, "geo/player.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HunterEntity object) {
        // ★ ここが重要：プレイヤーのスキンを取得する
        // 変身中のプレイヤーのUUIDなどからスキンを特定
        if (object.getPlayerUUID() != null) {
            return Minecraft.getInstance().getSkinManager()
                    .getInsecureSkinLocation(new com.mojang.authlib.GameProfile(object.getPlayerUUID(), null));
        }
        // デフォルト
        return new ResourceLocation("textures/entity/player/slim/alex.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HunterEntity animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "animations/hunter_animation.json");
    }
}