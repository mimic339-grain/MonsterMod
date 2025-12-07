package com.mimic.monstermod.gui.hunter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * スロットクリックだけ担当する透明ウィジェット
 */
public class HunterSlotWidget extends AbstractWidget {

    private final HunterSlot slot;

    public HunterSlotWidget(int x, int y, int size, HunterSlot slot) {
        super(x, y, size, size, Component.empty());
        this.slot = slot;
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        // 透明 → 描画しない
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        slot.handleClick(Minecraft.getInstance(), mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}
}
