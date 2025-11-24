package com.mimic.monstermod.gui;

import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
public class MonsterHpOverlay {

    private static final Minecraft mc = Minecraft.getInstance();

    /** IGuiOverlay に登録することで描画タイミングが安定 */
    public static final IGuiOverlay MONSTER_HP_OVERLAY = MonsterHpOverlay::drawOverlay;

    private static boolean shouldDisplayBar() {
        return mc.player != null && !mc.options.hideGui;
    }

    public static void drawOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        if (!shouldDisplayBar()) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // 変身中のみ表示
            if (!trans.isTransformed() || trans.getIdentity() == null) return;

            // HP取得
            String identityId = trans.getIdentity().getId();
            double currentHP = MonsterTransformUtil.getIdentityHP(player, identityId);
            double maxHP = MonsterTransformUtil.getIdentityMaxHP(player);

            // HPバーの描画位置
            int barWidth = 100;
            int barHeight = 8;
            int x = 120; // 左寄せ
            int y = height - 39; // Vanilla HPバーより少し上

            // 背景（黒）
            graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF000000);

            // HP割合
            double hpRatio = currentHP / maxHP;

            // HPゲージ色（緑→黄→赤）
            int hpColor;
            if (hpRatio > 0.5) {
                hpColor = 0xFF00FF00; // 緑
            } else if (hpRatio > 0.15) {
                hpColor = 0xFFFFFF00; // 黄
            } else {
                hpColor = 0xFFFF0000; // 赤
            }

            // HPゲージ描画
            int filled = (int) (barWidth * hpRatio);
            graphics.fill(x, y, x + filled, y + barHeight, hpColor);

            // HP数値を白文字で描画、周囲を黒で縁取り
            String hpText = String.format("%.0f / %.0f", currentHP, maxHP);
            int textWidth = mc.font.width(hpText);
            int textX = x + (barWidth - textWidth) / 2;
            int textY = y; // バー上に少し被せる

            int outlineColor = 0xFF000000; // 黒
            int mainColor = 0xFFFFFFFF;    // 白

            // 黒縁取りを四方向に描画
            graphics.drawString(mc.font, hpText, textX - 1, textY, outlineColor, false);
            graphics.drawString(mc.font, hpText, textX + 1, textY, outlineColor, false);
            graphics.drawString(mc.font, hpText, textX, textY - 1, outlineColor, false);
            graphics.drawString(mc.font, hpText, textX, textY + 1, outlineColor, false);

            // 白文字を中央に描画
            graphics.drawString(mc.font, hpText, textX, textY, mainColor, false);
        });
    }
}
