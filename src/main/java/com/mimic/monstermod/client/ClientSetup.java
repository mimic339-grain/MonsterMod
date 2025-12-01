package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.PlayerAnimation;
import dev.kosmx.playerAnim.impl.compat.skinLayers.SkinLayersTransformer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 1. ModifierLayer 登録
        PlayerAnimation.registerAnimations();

        try {
            Class.forName("dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI");
            SkinLayersTransformer.init(MonsterMod.LOGGER);
        } catch (ClassNotFoundException e) {
            MonsterMod.LOGGER.info("SkinLayers not found, skipping SkinLayersTransformer.");
        }
    }
}
