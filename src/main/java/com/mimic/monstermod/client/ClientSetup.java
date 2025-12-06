package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.PlayerAnimation;
import dev.kosmx.playerAnim.impl.compat.skinLayers.SkinLayersTransformer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 1. ModifierLayer 登録
        PlayerAnimation.registerAnimations();

        // 2. SkinLayers の安全な optional load
        if (ModList.get().isLoaded("skinlayers")) {
            try {
                // API が存在するかチェック
                Class.forName("dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI");

                // 初期化
                SkinLayersTransformer.init(MonsterMod.LOGGER);
                MonsterMod.LOGGER.info("SkinLayersTransformer initialized.");

            } catch (Throwable e) {
                // バージョン違い or API が変わった場合も安全にスキップ
                MonsterMod.LOGGER.warn("SkinLayers is present but API not found. Skipping SkinLayersTransformer.");
            }
        } else {
            MonsterMod.LOGGER.info("SkinLayers not found, skipping SkinLayersTransformer.");
        }
    }
}
