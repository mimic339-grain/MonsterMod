package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetHunterSkillPacket;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.skill.hunter.HunterSkill.HunterSkillSlot;
import com.mimic.monstermod.skill.hunter.HunterSkillRegistry;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class SkillSelectionScreen extends Screen {
    private final HunterSkillSlot currentSlot;
    private final WeaponCategory currentWeapon;
    private List<HunterSkill> sortedList = new ArrayList<>();
    private boolean isLocked = false;

    private float scrollOffset = 0;
    private final int windowWidth = 240;
    private final int windowHeight = 180;

    public SkillSelectionScreen(int slotIndex, WeaponCategory weapon) {
        super(Component.literal("Hunter Skill Selection"));

        this.currentSlot = switch(slotIndex) {
            case 3 -> HunterSkillSlot.DODGE;
            case 1 -> HunterSkillSlot.SKILL_2;
            case 2 -> HunterSkillSlot.SKILL_3;
            default -> HunterSkillSlot.SKILL_1;
        };
        this.currentWeapon = weapon;
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
                var identity = cap.getIdentity();
                if (identity != null) {
                    // slotIndex は 0,1,2,3(Dodge) なのでそのまま使える
                    this.isLocked = identity.getCooldown(slotIndex) > 0 || identity.isEffectActive(slotIndex);
                }
            });
        }
    }

    @Override
    protected void init() {
        List<HunterSkill> available = new ArrayList<>();
        List<HunterSkill> unavailable = new ArrayList<>();
        for (HunterSkill skill : HunterSkillRegistry.getAll()) {
            if (skill.canFitIn(currentSlot, currentWeapon)) available.add(skill);
            else unavailable.add(skill);
        }
        sortedList.clear();
        sortedList.addAll(available);
        sortedList.addAll(unavailable);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);

        int left = (this.width - windowWidth) / 2;
        int top = (this.height - windowHeight) / 2;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 50);

        gui.fill(left, top, left + windowWidth, top + windowHeight, 0xEE111111);
        gui.renderOutline(left, top, windowWidth, windowHeight, 0xFFFFFFFF);

        gui.enableScissor(left + 5, top + 5, left + windowWidth - 5, top + windowHeight - 5);

        int itemY = top + 5 - (int)scrollOffset;
        for (HunterSkill skill : sortedList) {
            boolean canEquip = skill.canFitIn(currentSlot, currentWeapon);
            int boxX = left + 8;
            int boxWidth = windowWidth - 25;

            int color = canEquip ? 0x4400FF00 : 0x44888888;
            gui.fill(boxX, itemY, boxX + boxWidth, itemY + 35, color);

            if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= itemY && mouseY <= itemY + 35) {
                gui.fill(boxX, itemY, boxX + boxWidth, itemY + 35, 0x22FFFFFF);
            }

            gui.blit(skill.getIcon(), boxX + 5, itemY + 7, 0, 0, 20, 20, 20, 20);
            gui.drawString(font, skill.getName(), boxX + 30, itemY + 10, canEquip ? 0xFFFFFF : 0xAAAAAA);

            drawInfoButton(gui, boxX + boxWidth - 18, itemY + 10, mouseX, mouseY);

            itemY += 40;
        }
        gui.disableScissor();

        drawScrollBar(gui, left + windowWidth - 10, top + 5);

        gui.pose().popPose();
        super.render(gui, mouseX, mouseY, partialTick);
        if (isLocked) {
            // ロックされている警告を表示
            gui.drawCenteredString(font, "クールダウン中は変更できません！", this.width / 2, top - 15, 0xFF5555);
        }
    }

    private void drawInfoButton(GuiGraphics gui, int x, int y, int mx, int my) {
        boolean hover = mx >= x && mx <= x + 15 && my >= y && my <= y + 15;
        gui.fill(x, y, x + 15, y + 15, hover ? 0xFFFFFFFF : 0xAAFFFFFF);
        gui.drawString(font, "i", x + 6, y + 4, 0x000000, false);
    }

    // ★ 重複を解消し、1つにまとめました
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int left = (this.width - windowWidth) / 2;
        int top = (this.height - windowHeight) / 2;
        int itemY = top + 5 - (int)scrollOffset;
        if (isLocked) return false;
        for (HunterSkill skill : sortedList) {
            int boxX = left + 8;
            int boxWidth = windowWidth - 25;

            // 「i」ボタンの判定
            int infoX = boxX + boxWidth - 18;
            int infoY = itemY + 10;
            if (mx >= infoX && mx <= infoX + 15 && my >= infoY && my <= infoY + 15) {
                // TODO: Info画面（詳細画面）を開く処理
                return true;
            }

            // スキル選択の判定
            if (mx >= boxX && mx <= boxX + boxWidth && my >= itemY && my <= itemY + 35) {
                if (skill.canFitIn(currentSlot, currentWeapon)) {
                    // ★ ModMessages.sendToServer を使用
                    ModMessages.sendToServer(new C2S_SetHunterSkillPacket(currentSlot, skill.getId()));
                    this.onClose();
                    return true;
                }
            }
            itemY += 40;
        }
        return super.mouseClicked(mx, my, button);

    }

    private void drawScrollBar(GuiGraphics gui, int x, int y) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;
        int barHeight = windowHeight - 10;
        int thumbHeight = Math.max(20, (int)((float)barHeight * windowHeight / (sortedList.size() * 40)));
        int thumbPos = (int)((float)(barHeight - thumbHeight) * scrollOffset / maxScroll);
        gui.fill(x, y, x + 4, y + barHeight, 0xFF333333);
        gui.fill(x, y + thumbPos, x + 4, y + thumbPos + thumbHeight, 0xFFAAAAAA);
    }

    private int getMaxScroll() {
        return Math.max(0, (sortedList.size() * 40) - (windowHeight - 10));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollOffset = Mth.clamp(this.scrollOffset - (float)delta * 20, 0, getMaxScroll());
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}