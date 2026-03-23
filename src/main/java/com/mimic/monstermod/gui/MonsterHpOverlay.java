package com.mimic.monstermod.gui;

import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
            // 変身中かつIdentityが存在する場合のみ表示
            if (!trans.isTransformed() || trans.getIdentity() == null) return;

            // HP情報の取得
            String identityId = trans.getIdentity().getId();
            double currentHP = MonsterTransformUtil.getIdentityHP(player, identityId);
            double maxHP = MonsterTransformUtil.getIdentityMaxHP(player);
            if (maxHP <= 0) return;

            // --- 1. レイアウト計算 (通常のハートの位置を狙う) ---
            int barWidth = 80;  // 前者のコードと同じ大きめの幅
            int barHeight = 8;   // 前者のコードと同じ高さ

            // 【相対座標】画面中央 (width/2) から左にオフセット。
            // バニラのハート表示領域（-91ピクセル付近）にバーを配置
            int x = (width / 2) - 91 - (barWidth / 2) + 41; // ハートの並びに合うよう微調整
            // ホットバーの少し上。バニラのHPバーより少しだけ高い位置
            int y = height - 40;

            // --- 2. 背景・外枠の描画 ---
            // 外枠を1ピクセル大きく描画して「枠」に見せる
            graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
            // ホットバー風の半透明な濃いグレー背景
            graphics.fill(x, y, x + barWidth, y + barHeight, 0xAA101010);

            // --- 3. HPゲージの描画 ---
            double hpRatio = Math.max(0.0, Math.min(1.0, currentHP / maxHP));

            int hpColor;
            if (hpRatio > 0.5) {
                hpColor = 0xFF00FF00; // 緑
            } else if (hpRatio > 0.15) {
                hpColor = 0xFFFFFF00; // 黄
            } else {
                hpColor = 0xFFFF0000; // 赤
            }

            int filledWidth = (int) (barWidth * hpRatio);
            if (filledWidth > 0) {
                graphics.fill(x, y, x + filledWidth, y + barHeight, hpColor);
            }


            // --- 4. HP数値の描画 (バーの真上・中央に配置) ---
            String hpText = String.format("%.0f / %.0f", currentHP, maxHP);
            Font font = mc.font;

            graphics.pose().pushPose();

            float textScale = 0.8f;
            graphics.pose().scale(textScale, textScale, textScale);

            // 【左右中央の計算】
            // (バーの左端 + バーの幅の半分) でバーの中心を出し、そこから (テキストの幅の半分) を引く
            float scaledTextWidth = font.width(hpText) * textScale;
            float textX = (x + (barWidth / 2f) - (scaledTextWidth / 2f)) / textScale;

            // 【上下位置の計算】
            // バーの上端 (y) から、少し隙間（4ピクセル分）を空けて上に配置
            // textScaleで割ることで、スケール後の正しい座標になります
            float textY = (y - (font.lineHeight * textScale) - 1) / textScale;

            // ドロップシャドウ付きで描画
            graphics.drawString(font, hpText, (int)textX, (int)textY, 0xFFFFFFFF, true);

            graphics.pose().popPose();
        });
    }
}