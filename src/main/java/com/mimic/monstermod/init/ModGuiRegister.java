package com.mimic.monstermod.init;

import com.mimic.monstermod.gui.MonsterHpOverlay;
import com.mimic.monstermod.gui.MonsterSkillOverlay;
import com.mimic.monstermod.gui.hunter.HunterSkillOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModGuiRegister {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {


        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "skill_hud", MonsterSkillOverlay.HUD_SKILLS);
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "monster_hp", MonsterHpOverlay.MONSTER_HP_OVERLAY);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "hunter_skill_hud", HunterSkillOverlay.HUD_HUNTER_SKILLS);
    }
}