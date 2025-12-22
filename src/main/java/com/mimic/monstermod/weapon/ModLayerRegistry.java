package com.mimic.monstermod.weapon;

import com.mimic.monstermod.MonsterMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = MonsterMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class ModLayerRegistry {
    /*
    @SubscribeEvent
    public static void addCustomLayers(EntityRenderersEvent.AddLayers event) {

        PlayerRenderer defaultRenderer = event.getSkin("default");
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(
                    new CustomItemInHandLayer(defaultRenderer)
            );
        }

        PlayerRenderer slimRenderer = event.getSkin("slim");
        if (slimRenderer != null) {
            slimRenderer.addLayer(
                    new CustomItemInHandLayer(slimRenderer)
            );
        }
    }*/
}
