package com.mimic.monstermod.client;

import com.mimic.monstermod.item.BaseMonsterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
public class HotbarCooldownRenderer {

    private static final int SLOT_SIZE = 20; // 枠サイズ（アイテムより少し大きめ）
    private static final int THICKNESS = 2;  // 枠線の太さ
    private static final int OFFSET_Y = -1;  // 上方向微調整

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();

        int slotCount = 9;
        int startX = (mc.getWindow().getGuiScaledWidth() - SLOT_SIZE * slotCount) / 2;
        int startY = mc.getWindow().getGuiScaledHeight() - SLOT_SIZE + OFFSET_Y;

        for (int i = 0; i < slotCount; i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BaseMonsterItem monsterItem) {
                long remaining = monsterItem.getCooldown().getRemaining(player.getUUID().toString());
                int color = remaining > 0 ? 0x80FF0000 : 0x8000FF00; // 赤:クールダウン中 / 緑:使用可能

                int x = startX + i * SLOT_SIZE;
                int y = startY;

                // 枠線の描画（上・下・左・右）
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + THICKNESS, color); // 上
                guiGraphics.fill(x, y + SLOT_SIZE - THICKNESS, x + SLOT_SIZE, y + SLOT_SIZE, color); // 下
                guiGraphics.fill(x, y, x + THICKNESS, y + SLOT_SIZE, color); // 左
                guiGraphics.fill(x + SLOT_SIZE - THICKNESS, y, x + SLOT_SIZE, y + SLOT_SIZE, color); // 右
            }
        }
    }
}
