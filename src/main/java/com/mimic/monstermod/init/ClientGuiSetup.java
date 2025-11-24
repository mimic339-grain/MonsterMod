package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.gui.MonsterHpOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientGuiSetup {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        // Vanilla HP バーの上に自作HPバーを描画
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "monster_hp", MonsterHpOverlay.MONSTER_HP_OVERLAY);
    }
}
