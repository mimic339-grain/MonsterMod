package com.mimic.monstermod.client;

import com.mimic.monstermod.gui.MonsterHpOverlay;
import com.mimic.monstermod.gui.SkillOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModGuiOverlays {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {


        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "skill_hud", SkillOverlay.HUD_SKILLS);
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "monster_hp", MonsterHpOverlay.MONSTER_HP_OVERLAY);
    }
}