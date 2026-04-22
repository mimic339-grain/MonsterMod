package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.keybind.HunterKeyBindings;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.skill.hunter.HunterSkill.HunterSkillSlot;
import com.mimic.monstermod.skill.hunter.HunterSkillRegistry;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HunterSkillOverlay {

    public static final IGuiOverlay HUD_HUNTER_SKILLS = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
            if (!cap.isActive()) return;

            // Identityの取得（判定用。取れなくてもアイコン描画は続行する）
            BaseIdentity identity = cap.getIdentity();

            int spacing = 28;
            int xRightEdge = (width / 2) + 95;
            int baseY = height - 21;

            HunterSkillSlot[] slots = {
                    HunterSkillSlot.SKILL_1,
                    HunterSkillSlot.SKILL_2,
                    HunterSkillSlot.SKILL_3,
                    HunterSkillSlot.DODGE
            };

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200); // 重なり防止

            for (int i = 0; i < slots.length; i++) {
                int drawX = xRightEdge + (i * spacing);
                // IDは一番信頼できる「Capability」から直接取る
                SkillId currentId = cap.getEquippedSkill(slots[i]);

                renderHunterSlot(guiGraphics, drawX, baseY, i, slots[i], currentId, identity, mc.font);
            }

            guiGraphics.pose().popPose();
        });
    };

    //todo 現状のcooldownはhunter用にあっていないため永遠に緑色の枠組みになる　またスキル発動中が藍色枠組みにならない　アイコンの上にレイヤーがほしいこれはクールダウン中であれば赤色枠組みとアイコンの上のレイヤーが暗くなって上から下に明るくなるようにする　つまりクールダウン中に暗いlayerをセットしてクールダウンによってだんだん明るくする　また、アイコンの上に秒数で何秒ってかいて残り5秒から少数第一を見せる4.9 0.2とかね　
    private static void renderHunterSlot(GuiGraphics gui, int x, int y, int index, HunterSkillSlot slot, SkillId skillId, @Nullable BaseIdentity identity, Font font) {
        HunterSkill skill = (skillId != null) ? HunterSkillRegistry.get(skillId) : null;

        int currentCd = 0;
        int defaultCd = 60;
        boolean isEffectActive = false;
        boolean isAnyActive = false;
        boolean isComboWindow = false;

        if (identity != null) {
            currentCd = identity.getCooldown(index);
            defaultCd = identity.getDefaultCooldown(index);
            isEffectActive = identity.isEffectActive(index);
            isAnyActive = identity.isAnySkillActive();
            isComboWindow = identity.isComboWindowActive();
            if (defaultCd <= 0 && skill != null) defaultCd = skill.getCooldownTicks();
        }

        // --- 1. カラー定義 (修正：Readyを黄緑に) ---
        int colorReady = 0xAA33FF00;
        int colorActive = 0xAA00CCFF;   // 落ち着いた青
        int colorCooldown = 0xAAFF4444; // ソフトな赤
        int colorLocked = 0xAA555555;   // ロック中
        int colorFrameDefault = 0x44FFFFFF; // 未装備等のデフォルト

        int frameColor = colorFrameDefault;
        if (skill != null) {
            if (isEffectActive) frameColor = colorActive;
            else if (currentCd > 0) frameColor = colorCooldown;
            else {
                boolean canCast;
                SkillType.Category category = skill.getCategory();
                if (category == SkillType.Category.CANCEL) canCast = true;
                else if (category == SkillType.Category.COMBO) canCast = isComboWindow;
                else if (category == SkillType.Category.NORMAL) {
                    boolean isDashing = false;
                    if (identity != null) {
                        SkillId[] ids = identity.getSkillIds();
                        for (int i = 0; i < ids.length; i++) {
                            if (ids[i] == null) continue;
                            SkillLead l = SkillLeadRegistry.getNullable(ids[i]);
                            if (l != null && l.category == SkillType.Category.DASH && identity.getComboWindow(i) > 0) {
                                isDashing = true; break;
                            }
                        }
                    }
                    canCast = !isAnyActive || isDashing;
                } else canCast = !isAnyActive;
                frameColor = canCast ? colorReady : colorLocked;
            }
        }

        gui.pose().pushPose();
        gui.pose().translate(x, y, 200);
        int s = 20;

        // --- 2. 角を削った背景 (修正：真っ黒からダークグレーへ) ---
        int bgGray = 0xCC252525; // 少し明るめのチャコールグレー
        gui.fill(1, 0, s - 1, s, bgGray);
        gui.fill(0, 1, s, s - 1, bgGray);

        // --- 3. アイコンとクールダウン演出 ---
        if (skill != null) {
            gui.blit(skill.getIcon(), 3, 3, 0, 0, s - 6, s - 6, s - 6, s - 6);

            if (currentCd > 0 && defaultCd > 0 && !isEffectActive) {
                float ratio = (float) currentCd / defaultCd;
                int shadowH = (int) ((s - 2) * ratio);
                int shadowY = s - 1 - shadowH;
                gui.fill(1, Math.max(1, shadowY), s - 1, s - 1, 0x99001122);
                if (shadowY < 1) gui.fill(2, 0, s - 2, 1, 0x99001122);
            }
        }

        // --- 4. 角を削った外枠 ---
        int c = frameColor;
        gui.fill(1, 0, s - 1, 1, c);
        gui.fill(1, s - 1, s - 1, s, c);
        gui.fill(0, 1, 1, s - 1, c);
        gui.fill(s - 1, 1, s, s - 1, c);
        // 角の点
        gui.fill(1, 1, 2, 2, c);
        gui.fill(s - 2, 1, s - 1, 2, c);
        gui.fill(1, s - 2, 2, s - 1, c);
        gui.fill(s - 2, s - 2, s - 1, s - 1, c);

        // --- 5. テキスト描画 (秒数) ---
        String cdText = null;
        int cdTextColor = 0xFFFFFFFF;
        if (isEffectActive) {
            SkillLead lead = SkillLeadRegistry.getNullable(skillId);
            if (lead != null) {
                int remaining = identity.getLockTime(index) - (lead.recoveryTicks + 1);
                if (remaining > 0) {
                    float sec = remaining / 20.0f;
                    cdText = (remaining <= 100) ? String.format("%.1f", sec) : String.valueOf((int) Math.ceil(sec));
                    cdTextColor = 0xFF00E6FF;
                }
            }
        } else if (currentCd > 0) {
            float sec = currentCd / 20.0f;
            cdText = (currentCd <= 100) ? String.format("%.1f", sec) : String.valueOf((int) Math.ceil(sec));
        }

        if (cdText != null) {
            gui.pose().pushPose();
            gui.pose().translate(s / 2f, s / 2f - 2, 10);
            gui.pose().scale(0.7f, 0.7f, 0.7f);
            gui.drawCenteredString(font, cdText, 0, 0, cdTextColor);
            gui.pose().popPose();
        }

        // スキル名
        gui.pose().pushPose();
        gui.pose().translate(s / 2f, -7, 10);
        gui.pose().scale(0.6f, 0.6f, 0.6f);
        gui.drawCenteredString(font, (skill != null ? skill.getName() : getDefaultSlotName(slot)), 0, 0, 0xBBCCCCCC);
        gui.pose().popPose();

        // --- 6. キーバインド (ボックス付きレイアウト維持) ---
        String keyLabel = getBindingLabel(index, slot);
        int labelWidth = font.width(keyLabel);

        gui.pose().pushPose();
        float keyBoxScale = 0.5f;
        int boxPadding = 2;
        // スロット右下に配置
        gui.pose().translate(s + 2, s - 2, 300);
        gui.pose().scale(keyBoxScale, keyBoxScale, keyBoxScale);

        int bgX = -(labelWidth + boxPadding * 2);
        int bgY = -10;
        int bgW = labelWidth + boxPadding * 2;
        int bgH = 12;

        // ボックス背景と枠
        gui.fill(bgX, bgY, bgX + bgW, bgY + bgH, 0xDD222222); // 背景
        gui.fill(bgX - 1, bgY - 1, bgX + bgW + 1, bgY, 0xFF888888); // 上
        gui.fill(bgX - 1, bgY + bgH, bgX + bgW + 1, bgY + bgH + 1, 0xFF888888); // 下
        gui.fill(bgX - 1, bgY, bgX, bgY + bgH, 0xFF888888); // 左
        gui.fill(bgX + bgW, bgY, bgX + bgW + 1, bgY + bgH, 0xFF888888); // 右

        gui.drawString(font, keyLabel, bgX + boxPadding, bgY + 2, 0xFFFFFFFF, false);
        gui.pose().popPose();

        gui.pose().popPose();
    }

    private static String getDefaultSlotName(HunterSkillSlot slot) {
        return switch (slot) {
            case SKILL_1 -> "SKILL 1";
            case SKILL_2 -> "SKILL 2";
            case SKILL_3 -> "SKILL 3";
            case DODGE -> "回避";
        };
    }
    /**
     * インデックスとスロットから、現在割り当てられているキーのラベルを取得する
     */
    private static String getBindingLabel(int index, HunterSkillSlot slot) {
        // 回避(DODGE)スロットの場合
        if (slot == HunterSkillSlot.DODGE) {
            if (HunterKeyBindings.DODGE_KEY != null) {
                return HunterKeyBindings.DODGE_KEY.getTranslatedKeyMessage().getString().toUpperCase();
            }
            return "L";
        }

        // Skill 1, 2, 3 の場合
        if (index >= 0 && index < HunterKeyBindings.SKILL_KEYS.length) {
            if (HunterKeyBindings.SKILL_KEYS[index] != null) {
                return HunterKeyBindings.SKILL_KEYS[index].getTranslatedKeyMessage().getString().toUpperCase();
            }
        }

        return String.valueOf(index + 1);
    }
}