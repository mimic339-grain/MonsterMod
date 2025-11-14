package com.mimic.monstermod.client;

import com.mimic.monstermod.gui.GuiSkills;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModGuiOverlays {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        IGuiOverlay monsterOverlay = GuiSkills.OVERLAY;
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "monstermod_monster_skills", monsterOverlay);
    }
}
