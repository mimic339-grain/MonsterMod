package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetHunterSlotPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hunter装備 GUI
 * ----------------
 * InventoryScreen 上に Hunter専用 WeaponSlot を描画
 * Widget を追加してクリック処理を担当
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class HunterMeny {

    private static final ResourceLocation EQUIP_TEX =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/gui/equip.png");

    private static WeaponSlot slot;
    private static WeaponSlotWidget slotWidget;

    /** GUI描画（毎フレーム） */
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

        // GUI背景描画
        gui.blit(EQUIP_TEX, baseX, baseY, 0, 0, 120, 120, 120, 120);

        if (slot == null) {
            slot = new WeaponSlot(baseX + 56, baseY + 8, 16);
        }

        slot.setPosition(baseX + 56, baseY + 8);
        slot.render(mc, gui);
    }

    /** Widget追加（クリック処理） */
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

        if (slot == null) slot = new WeaponSlot(slotX, slotY, 16);
        slot.setPosition(slotX, slotY);

        if (slotWidget != null) {
            event.removeListener(slotWidget);
        }

        slotWidget = new WeaponSlotWidget(slotX, slotY, 16, slot);
        event.addListener(slotWidget);

        // GUI初期化時にサーバーから最新データを要求
        ModMessages.sendToServer(C2S_SetHunterSlotPacket.createRequest());
    }

    /**
     * サーバーから同期パケットを受け取ったときに呼ぶ
     * (S2C_SyncHunterSlotPacket から呼ばれる)
     */
    public static void onServerSync(ItemStack stack) {
        if (slot != null) {
            slot.updateFromServer(stack);
        }
    }
}
