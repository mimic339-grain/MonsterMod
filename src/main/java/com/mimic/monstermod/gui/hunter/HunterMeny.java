package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class HunterMeny {

    private static final ResourceLocation EQUIP_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/gui/equip.png");

    private static HunterSlot slot;
    private static HunterSlotWidget slotWidget;

    /** GUI 描画（毎フレーム） */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        HunterTransformation ht =
                mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).orElse(null);
        if (ht == null || !ht.isActive()) return;

        GuiGraphics gui = event.getGuiGraphics();

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        int baseX = left - 80;
        int baseY = top;

        gui.blit(EQUIP_TEX, baseX, baseY, 0, 0, 120, 120, 120, 120);

        // スロット初期化
        if (slot == null) {
            slot = new HunterSlot(baseX + 56, baseY + 8, 16);
        }

        // 毎フレーム位置更新
        slot.setPosition(baseX + 56, baseY + 8);

        // アイテム描画
        slot.render(mc, gui);
    }

    /** Widget 追加（クリック処理） */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        HunterTransformation ht =
                mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).orElse(null);
        if (ht == null || !ht.isActive()) return;

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        int slotX = left - 80 + 56;
        int slotY = top + 8;

        if (slot == null) slot = new HunterSlot(slotX, slotY, 16);
        slot.setPosition(slotX, slotY);

        if (slotWidget != null) {
            event.removeListener(slotWidget);
        }

        slotWidget = new HunterSlotWidget(slotX, slotY, 16, slot);
        event.addListener(slotWidget);
    }
}
