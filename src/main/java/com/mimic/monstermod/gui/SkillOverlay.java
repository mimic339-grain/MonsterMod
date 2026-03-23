package com.mimic.monstermod.gui;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.impl.MonsterKeyBindings;
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

public class SkillOverlay {

    public static final IGuiOverlay HUD_SKILLS = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            BaseMonsterIdentity identity = trans.getIdentity();
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

    private static void renderSlot(GuiGraphics gui, int x, int y, int index, BaseMonsterIdentity identity, Font font) {
        SkillId[] skills = identity.getSkillIds();
        if (index >= skills.length) return;

        SkillId skillId = skills[index];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        int currentCd = identity.getCooldown(index);
        int defaultCd = identity.getDefaultCooldown(index);
        // --- 1. 枠色の決定 ---
        int frameColor;
        if (currentCd > defaultCd) {
            frameColor = 0xFF00AAFF; // 青: アクティブ
        } else if (currentCd > 0) {
            frameColor = 0xFFFF0000; // 赤: クールダウン
        } else {
            if (lead.category == SkillType.Category.NORMAL && identity.isAnyNormalSkillActive()) {
                frameColor = 0xFF555555; // 灰: ロック
            } else {
                frameColor = 0xFF00FF00; // 緑: OK
            }
        }

        // --- 2. 描画 (枠線と背景を独立して描画) ---

        // ★背景を先に描画 (18x18)。半透明グレーでも下に色がないので濁りません
        gui.fill(x + 1, y + 1, x + 19, y + 19, 0x90101010);

        // ★枠線を「4本の線」として描画。これで背景部分には一切 frameColor が入り込みません
        gui.fill(x, y, x + 20, y + 1, frameColor);             // 上
        gui.fill(x, y + 19, x + 20, y + 20, frameColor);       // 下
        gui.fill(x, y + 1, x + 1, y + 19, frameColor);         // 左
        gui.fill(x + 19, y + 1, x + 20, y + 19, frameColor);   // 右

        // クールダウンゲージ
        if (currentCd > 0) {
            // maxTotal を defaultCd (設定された最大リロード時間) のみにする
            // これで「リロード開始 = ゲージ満タン」から綺麗に減っていくようになります
            float ratio = (float) currentCd / Math.max(1, defaultCd);
            int fillHeight = (int) (18 * ratio);
            gui.fill(x + 1, y + 19 - fillHeight, x + 19, y + 19, 0x44FFFFFF);
        }

        // --- 3. キーラベル ---
        String keyLabel = getBindingLabel(index);
        gui.drawCenteredString(font, keyLabel, x + 10, y + 6, 0xFFFFFFFF);

        // --- 4. スキル名の動的スケール ---
        String name = skillId.location().getPath().replace("test_", "").toUpperCase();
        gui.pose().pushPose();

        // 文字数によるスケール判定
        float finalScale = (name.length() >= 6) ? 0.4f : 0.6f;

        gui.pose().scale(finalScale, finalScale, finalScale);
        float nameX = (x + 10) / finalScale;
        float nameY = (y - 6) / finalScale;

        gui.drawCenteredString(font, name, (int) nameX, (int) nameY, 0xFFCCCCCC);
        gui.pose().popPose();
    }

    private static String getBindingLabel(int index) {
        if (index >= 0 && index < MonsterKeyBindings.SKILL_KEYS.length) {
            return MonsterKeyBindings.SKILL_KEYS[index].getTranslatedKeyMessage().getString().toUpperCase();
        }
        return String.valueOf(index + 1);
    }
}