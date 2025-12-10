package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetHunterSlotPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * WeaponSlotWidget
 * GUI上のWeaponSlot用Widget
 * - クリックでWeaponItemをWeaponSlotに装備
 * - WeaponItem以外は装備不可
 * - サーバー同期あり
 */
public class WeaponSlotWidget extends AbstractWidget {

    private final WeaponSlot slot;
    private static WeaponSlot CLIENT_SLOT;

    public WeaponSlotWidget(int x, int y, int size, WeaponSlot slot) {
        super(x, y, size, size, Component.empty());
        this.slot = slot;
        CLIENT_SLOT = slot;
    }

    @Override
    protected void renderWidget(net.minecraft.client.gui.GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        // Widget自体は透明
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        HunterTransformation hunter = mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).orElse(null);
        if (hunter == null || !hunter.isActive()) return false;

        // Inventory上でドラッグしているアイテムを取得
        ItemStack clickedItem = mc.player.containerMenu.getCarried();

        // ドラッグしていなければ手持ちアイテムを使用
        if (clickedItem.isEmpty()) {
            clickedItem = mc.player.getMainHandItem();
        }

        if (clickedItem.isEmpty() || !(clickedItem.getItem() instanceof WeaponItem)) {
            mc.player.displayClientMessage(Component.literal("武器だけ装備できます"), true);
            return true;
        }

        // サーバー側で装備・既存装備インベントリ戻し処理を行う
        ModMessages.sendToServer(new C2S_SetHunterSlotPacket(clickedItem.copy()));

        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    public static WeaponSlot getClientSlot() {
        return CLIENT_SLOT;
    }
}
