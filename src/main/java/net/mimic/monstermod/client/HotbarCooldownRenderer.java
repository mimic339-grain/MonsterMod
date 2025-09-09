package net.mimic.monstermod.client;

import net.mimic.monstermod.item.BaseMonsterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
public class HotbarCooldownRenderer {

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int startX = 10;
        int startY = mc.getWindow().getGuiScaledHeight() - 22;

        for (int i = 0; i < 9; i++) {
            var item = player.getInventory().getItem(i).getItem();
            if (item instanceof BaseMonsterItem monsterItem) {
                long remaining = monsterItem.getCooldown().getRemaining(player.getUUID().toString());
                int color = remaining > 0 ? 0x80FF0000 : 0x8000FF00; // 赤/緑半透明

                int x = startX + i * 20;
                int y = startY;

                // 枠線の太さ
                int thickness = 1;

                // 上辺
                guiGraphics.fill(x, y, x + 16, y + thickness, color);
                // 下辺
                guiGraphics.fill(x, y + 16 - thickness, x + 16, y + 16, color);
                // 左辺
                guiGraphics.fill(x, y, x + thickness, y + 16, color);
                // 右辺
                guiGraphics.fill(x + 16 - thickness, y, x + 16, y + 16, color);
            }
        }
    }
}
