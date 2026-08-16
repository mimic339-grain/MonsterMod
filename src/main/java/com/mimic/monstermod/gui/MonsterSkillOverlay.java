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

            int spacing = 25;
            int xStart = (width / 2) + 95;
            int baseY = height - 21;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);

            for (int i = 0; i < skillCount; i++) {
                int row = (i >= 6) ? 1 : 0;
                int col = (i >= 6) ? i - 6 : i;

                int drawX = xStart + (col * spacing);
                int drawY = baseY - (row * spacing);

                renderMonsterSlot(guiGraphics, drawX, drawY, i, identity, mc.font);
            }
            guiGraphics.pose().popPose();
        });
    };

    private static void renderMonsterSlot(GuiGraphics guiGraphics, int x, int y, int index, BaseIdentity identity, Font font) {
        SkillId skillId = identity.getSkillIds()[index];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        int currentCd = identity.getCooldown(index);
        int defaultCd = identity.getDefaultCooldown(index);
        // 「武装中」もスキル発動中と同じ青枠で見せる。
        // ボマーの殴打ボムのように、押しただけでは何も起きず次の行動で効くスキルがあるため、
        // 今その状態かどうかが分からないと使いようがない
        boolean isEffectActive = identity.isEffectActive(index) || identity.isArmed(index);
        boolean isAnyActive = identity.isAnySkillActive();
        boolean isComboWindow = identity.isComboWindowActive();
        int lockTime = identity.getLockTime(index);

        // --- 1. カラー定義 ---
        int colorReady = 0x8833FF00;
        int colorActive = 0x8800CCFF;
        int colorCooldown = 0x88FF4444;
        int colorLocked = 0x88555555;
        int colorComboWindow = 0x88FFFF00;

        int frameColor;
        if (isEffectActive) frameColor = colorActive;
        else if (currentCd > 0 || lockTime > 0) frameColor = colorCooldown;
        else {
            boolean canCast;
            if (lead.category == SkillType.Category.CANCEL) canCast = true;
            else if (lead.category == SkillType.Category.COMBO) canCast = isComboWindow;
            else if (lead.category == SkillType.Category.NORMAL) {
                boolean isDashing = false;
                for (SkillId sid : identity.getSkillIds()) {
                    SkillLead l = SkillLeadRegistry.getNullable(sid);
                    if (l != null && l.category == SkillType.Category.DASH && identity.getComboWindow(identity.findSkillIndex(sid)) > 0) {
                        isDashing = true; break;
                    }
                }
                canCast = !isAnyActive || isDashing;
            } else canCast = !isAnyActive;

            frameColor = canCast ? (lead.category == SkillType.Category.COMBO && isComboWindow ? colorComboWindow : colorReady) : colorLocked;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        int s = 20;

        // --- 背景 (黒透過) ---
        int bgTrans = 0x66151515;
        guiGraphics.fill(1, 0, s - 1, s, bgTrans);
        guiGraphics.fill(0, 1, s, s - 1, bgTrans);
        // --- 3. キーバインド & カテゴリ ---
        String keyLabel = getBindingLabel(index);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(s / 2f, s / 2f - 5, 10);
        float keyScale = (keyLabel.length() > 2) ? 0.5f : 0.7f;
        guiGraphics.pose().scale(keyScale, keyScale, keyScale);
        guiGraphics.drawCenteredString(font, keyLabel, 0, 0, 0xCCFFFFFF);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(s / 2f, s - 4, 12);
        guiGraphics.pose().scale(0.4f, 0.4f, 0.4f);
        int catColor = (lead.category == SkillType.Category.COMBO && isComboWindow) ? 0xCCFFFF00 : 0xCCBBBBBB;
        guiGraphics.drawCenteredString(font, lead.category.name(), 0, 0, catColor);
        guiGraphics.pose().popPose();

        // --- 4. クールダウン・プレビュー影演出 ---
        if ((currentCd > 0 || lockTime > 0) && !isEffectActive) {
            // lockTimeがある間はプレビュー期間なので、本来のCDゲージは止まっているか最大で維持
            float ratio = (currentCd > 0) ? (float) currentCd / defaultCd : 1.0f;
            int shadowH = (int) ((s - 2) * ratio);
            int shadowY = (s - 1) - shadowH;

            guiGraphics.fill(1, Math.max(1, shadowY), s - 1, s - 1, 0x99000000);
            if (shadowY < 1) guiGraphics.fill(2, 0, s - 2, 1, 0x99000000);
        } else if (isEffectActive) {
            guiGraphics.fill(1, 1, s - 1, s - 1, 0x3300CCFF);
        }

        // --- 5. 外枠 ---
        drawCustomFrame(guiGraphics, s, frameColor);

        //todo追加 --- 6. 秒数表示 (プレビュー:オレンジ isComboWindowtick=        int colorComboWindow = 0x88FFFF00;この色/ CD:白) ---
        renderTimeText(guiGraphics, font, s, index, identity, skillId, currentCd, isEffectActive);

        // --- 7. スキル名 ---
        String name = skillId.location().getPath().replace("test_", "").toUpperCase();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(s / 2f, -4, 10);
        guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
        guiGraphics.drawCenteredString(font, name, 0, 0, 0xBBCCCCCC);
        guiGraphics.pose().popPose();

        guiGraphics.pose().popPose();
    }

    private static void drawCustomFrame(GuiGraphics guiGraphics, int s, int c) {
        guiGraphics.fill(1, 0, s - 1, 1, c);
        guiGraphics.fill(1, s - 1, s - 1, s, c);
        guiGraphics.fill(0, 1, 1, s - 1, c);
        guiGraphics.fill(s - 1, 1, s, s - 1, c);
        guiGraphics.fill(1, 1, 2, 2, c);
        guiGraphics.fill(s - 2, 1, s - 1, 2, c);
        guiGraphics.fill(1, s - 2, 2, s - 1, c);
        guiGraphics.fill(s - 2, s - 2, s - 1, s - 1, c);
    }

    private static void renderTimeText(GuiGraphics guiGraphics, Font font, int s, int index, BaseIdentity identity, SkillId skillId, int currentCd, boolean isEffectActive) {
        String cdText = null;
        int color = 0xFFFFFFFF; // デフォルトは白

        int lockTime = identity.getLockTime(index);
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);

        if (lockTime > 0 && lead != null) {
            // タイムライン計算: [予兆] -> [効果(青枠)] -> [後隙]
            int effectEnd = lead.recoveryTicks + 1;
            int effectStart = effectEnd + lead.effectTicks;

            if (lockTime > effectStart) {
                /// todo追加　comboカテゴリーのスキルに対してcobowindotickをcoboのすきるのところに秒数表示じゃね
                // ★ プレビュー(予兆)時間：オレンジ色
                cdText = formatTime(lockTime - effectStart);
                color = 0xFFFF9900; // おしゃれなオレンジ
            } else if (isEffectActive) {
                // ★ 効果発動中：水色
                int remainingEffect = lockTime - effectEnd;
                if (remainingEffect > 0) {
                    cdText = formatTime(remainingEffect);
                    color = 0xFF00E6FF;
                }
            }
            // recovery期間（後隙）に入ると lockTime は残っているが、
            // 下記の currentCd 分岐に任せるか、あるいは白にシフトする
        }

        // プレビュー表示がなくて、クールダウンがある場合
        if (cdText == null && currentCd > 0) {
            cdText = formatTime(currentCd);
            color = 0xFFFFFFFF; // 通常クールダウンは白
        }

        if (cdText != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(s / 2f, s / 2f + 1, 50);
            float scale = 0.85f;
            guiGraphics.pose().scale(scale, scale, scale);
            guiGraphics.drawCenteredString(font, cdText, 0, 0, color);
            guiGraphics.pose().popPose();
        }
    }

    private static String formatTime(int ticks) {
        float sec = ticks / 20.0f;
        return (ticks <= 100) ? String.format("%.1f", sec) : String.valueOf((int) Math.ceil(sec));
    }

    private static String getBindingLabel(int index) {
        if (index >= 0 && index < MonsterKeyBindings.SKILL_KEYS.length) {
            String key = MonsterKeyBindings.SKILL_KEYS[index].getTranslatedKeyMessage().getString().toUpperCase();
            return key.replace("MOUSE BUTTON ", "M").replace("BUTTON ", "M");
        }
        return String.valueOf(index + 1);
    }
}