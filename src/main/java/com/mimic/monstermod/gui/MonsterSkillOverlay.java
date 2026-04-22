package com.mimic.monstermod.gui;

import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.keybind.MonsterKeyBindings;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class MonsterSkillOverlay {

    public static final IGuiOverlay HUD_SKILLS = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            BaseIdentity identity = trans.getIdentity();
            if (identity == null) return;

            SkillId[] skills = identity.getSkillIds();
            int skillCount = skills.length;
            if (skillCount == 0) return;

            // --- レイアウト定数 (右寄せ設定) ---
            int spacing = 22;
            int xRightEdge = (width / 2) + 95;
            int baseY = height - 21;

            for (int i = 0; i < skillCount; i++) {
                int row = (skillCount >= 7 && i >= 6) ? 1 : 0;
                int col = (skillCount >= 7 && i >= 6) ? i - 6 : i;

                int drawX = xRightEdge + (col * spacing);
                int drawY = baseY - (row * spacing);

                renderSlot(guiGraphics, drawX, drawY, i, identity, mc.font);
            }
        });
    };

    private static void renderSlot(GuiGraphics gui, int x, int y, int index, BaseIdentity identity, Font font) {
        SkillId[] skills = identity.getSkillIds();
        if (index >= skills.length) return;

        SkillId skillId = skills[index];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        int currentCd = identity.getCooldown(index);
        int defaultCd = identity.getDefaultCooldown(index);

        // --- 1. 枠色の決定 ---
        int frameColor;

        // 【最優先】クールダウン中はカテゴリに関わらず「赤」
        if (currentCd > 0) {
            frameColor = 0xFFFF0000;
        }
        // 2. CANCELカテゴリ：CD中でなければ「常に緑」
        else if (lead.category == SkillType.Category.CANCEL) {
            frameColor = 0xFF00FF00;
        }
        // 3. COMBOカテゴリ：コンボ受付中なら「緑」、それ以外は「灰色」
        else if (lead.category == SkillType.Category.COMBO) {
            frameColor = identity.isComboWindowActive() ? 0xFF00FF00 : 0xFF444444;
        }
        // 4. NORMALカテゴリ：何もしてない or DASH派生受付中なら「緑」、それ以外は「灰色」
        else if (lead.category == SkillType.Category.NORMAL) {
            boolean isDashing = false;
            for (int i = 0; i < skills.length; i++) {
                SkillLead l = SkillLeadRegistry.getNullable(skills[i]);
                if (l != null && l.category == SkillType.Category.DASH && identity.getComboWindow(i) > 0) {
                    isDashing = true;
                    break;
                }
            }
            frameColor = (!identity.isAnySkillActive() || isDashing) ? 0xFF00FF00 : 0xFF555555;
        }
        // 5. DASH, UNIQUE, その他：何か一つでもスキルが動いていたら「灰色」、暇なら「緑」
        else {
            frameColor = identity.isAnySkillActive() ? 0xFF555555 : 0xFF00FF00;
        }

        // --- 2. 描画処理 (背景・枠・ゲージ) ---
        // 背景
        gui.fill(x + 1, y + 1, x + 19, y + 19, 0x90101010);

        // 決定した frameColor で枠を描画
        gui.fill(x, y, x + 20, y + 1, frameColor);             // 上
        gui.fill(x, y + 19, x + 20, y + 20, frameColor);       // 下
        gui.fill(x, y + 1, x + 1, y + 19, frameColor);         // 左
        gui.fill(x + 19, y + 1, x + 20, y + 19, frameColor);   // 右

        // クールダウンゲージ
        if (currentCd > 0) {
            float ratio = (float) currentCd / Math.max(1, defaultCd);
            int fillHeight = (int) (18 * ratio);
            gui.fill(x + 1, y + 19 - fillHeight, x + 19, y + 19, 0x44FFFFFF);
        }

        // --- 3. ラベルと名前 (既存のまま) ---
        String keyLabel = getBindingLabel(index);
        gui.drawCenteredString(font, keyLabel, x + 10, y + 6, 0xFFFFFFFF);

        String name = skillId.location().getPath().replace("test_", "").toUpperCase();
        gui.pose().pushPose();
        float finalScale = (name.length() >= 6) ? 0.4f : 0.6f;
        gui.pose().scale(finalScale, finalScale, finalScale);
        gui.drawCenteredString(font, name, (int)((x + 10) / finalScale), (int)((y - 6) / finalScale), 0xFFCCCCCC);
        gui.pose().popPose();
    }

    private static String getBindingLabel(int index) {
        if (index >= 0 && index < MonsterKeyBindings.SKILL_KEYS.length) {
            return MonsterKeyBindings.SKILL_KEYS[index].getTranslatedKeyMessage().getString().toUpperCase();
        }
        return String.valueOf(index + 1);
    }
}