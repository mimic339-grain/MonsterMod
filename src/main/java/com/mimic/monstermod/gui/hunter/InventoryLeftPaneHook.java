package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class InventoryLeftPaneHook {

    private static final ResourceLocation EQUIP_BG =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/gui/equip.png");

    private static final int PANE_WIDTH = 120;
    private static final int PANE_HEIGHT = 120;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inv)) return;

        GuiGraphics gui = event.getGuiGraphics();

        int left = inv.getGuiLeft() - PANE_WIDTH + 36;
        int top  = inv.getGuiTop();

        gui.blit(
                EQUIP_BG,
                left,
                top,
                0, 0,
                PANE_WIDTH,
                PANE_HEIGHT,
                PANE_WIDTH,
                PANE_HEIGHT
        );
    }
}
