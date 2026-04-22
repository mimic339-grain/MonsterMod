package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.skill.hunter.HunterSkill.HunterSkillSlot;
import com.mimic.monstermod.skill.hunter.HunterSkillRegistry;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class InventoryLeftPaneHook {

    private static final ResourceLocation INVENTORY_LOCATION = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final int WEAPON_Y = 8;
    private static final int SKILL_START_Y = 35;
    private static final int DODGE_Y = 105;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inv)) return;

        Minecraft mc = Minecraft.getInstance();
        if (!HunterTransformation.isHunter(mc.player)) return;

        boolean bookVisible = inv.getRecipeBookComponent().isVisible();
        int xOffset = bookVisible ? 176 + 4 : -28;

        GuiGraphics gui = event.getGuiGraphics();
        int left = inv.getGuiLeft();
        int top = inv.getGuiTop();
        int mx = event.getMouseX();
        int my = event.getMouseY();

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 100);

        int paneX = left + xOffset - 4;
        int paneY = top + 4;
        int width = 24;
        int height = 124;

        // パネル背景と外枠
        gui.fill(paneX, paneY, paneX + width, paneY + height, 0xFFC6C6C6);
        gui.fill(paneX - 1, paneY - 1, paneX + width, paneY, 0xFFFFFFFF);
        gui.fill(paneX - 1, paneY, paneX, paneY + height, 0xFFFFFFFF);
        gui.fill(paneX + width, paneY - 1, paneX + width + 1, paneY + height + 1, 0xFF555555);
        gui.fill(paneX - 1, paneY + height, paneX + width, paneY + height + 1, 0xFF555555);

        int slotX = left + xOffset - 1;

        mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
            // 1. 武器スロット
            int wY = top + WEAPON_Y - 1;
            drawSlotWithHighlight(gui, slotX, wY, mx, my);
            ItemStack weapon = cap.getWeaponSlot();
            if (!weapon.isEmpty()) {
                gui.renderItem(weapon, slotX + 1, wY + 1);
                gui.renderItemDecorations(mc.font, weapon, slotX + 1, wY + 1);
            }

            BaseIdentity identity = cap.getIdentity();

            // 2. スキルスロット
            HunterSkillSlot[] skillSlots = {HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3};
            for (int i = 0; i < 3; i++) {
                int sY = top + SKILL_START_Y + (i * 20) - 1;
                drawSlotWithHighlight(gui, slotX, sY, mx, my);

                SkillId skillId = cap.getEquippedSkill(skillSlots[i]);
                renderSkillIconInInventory(gui, slotX, sY, skillId);

                // ★ クールダウンと秒数の描画
                if (identity != null) {
                    renderCooldownEffects(gui, mc.font, slotX, sY, i, skillId, identity);
                }
            }

            // 3. 回避スロット
            int dY = top + DODGE_Y - 1;
            drawSlotWithHighlight(gui, slotX, dY, mx, my);
            SkillId dodgeId = cap.getEquippedSkill(HunterSkillSlot.DODGE);
            renderSkillIconInInventory(gui, slotX, dY, dodgeId);

            // ★ 回避スロット(index 3)のクールダウンと秒数
            if (identity != null) {
                renderCooldownEffects(gui, mc.font, slotX, dY, 3, dodgeId, identity);
            }
        });

        gui.pose().popPose();
    }

    // ★ インベントリ用にアイコンを描画するヘルパーメソッド
    private static void renderSkillIconInInventory(GuiGraphics gui, int x, int y, SkillId skillId) {
        if (skillId == null) return;
        HunterSkill skill = HunterSkillRegistry.get(skillId);
        if (skill != null) {
            // スロットの枠(18x18)に合わせて少し小さめ(16x16)に描画、または枠いっぱいに描画
            // ここでは overlay と合わせて 20x20 の画像を 16x16 にフィットさせて描画します
            gui.blit(skill.getIcon(), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        }
    }
    private static void renderCooldownEffects(GuiGraphics gui, net.minecraft.client.gui.Font font, int x, int y, int index, SkillId skillId, BaseIdentity identity) {
        int currentCd = identity.getCooldown(index);
        int defaultCd = identity.getDefaultCooldown(index);
        boolean isEffectActive = identity.isEffectActive(index);

        if (currentCd <= 0 && !isEffectActive) return;

        int size = 18; // スロット枠のサイズ

        // 1. クールダウンレイヤー（HUDと同じく下から上に晴れる演出）
        if (currentCd > 0 && defaultCd > 0 && !isEffectActive) {
            float ratio = (float) currentCd / defaultCd;
            int shadowHeight = (int) ((size - 2) * ratio);
            int shadowStartY = (y + size - 1) - shadowHeight;

            // アイコンの上に暗いレイヤーを重ねる
            gui.fill(x + 1, shadowStartY, x + size - 1, y + size - 1, 0xAA000000);
            // クールダウン中はスロットを赤枠で強調（山口さんの以前の要望を反映）
            gui.renderOutline(x, y, size, size, 0x88FF0000);
        }

        // 2. 秒数テキストの描画
        String cdText = "";
        int textColor = 0xFFFFFFFF;

        if (isEffectActive) {
            // 発動中（青色）
            com.mimic.monstermod.skill.SkillLead lead = com.mimic.monstermod.skill.SkillLeadRegistry.getNullable(skillId);
            if (lead != null) {
                int remainingEffect = identity.getLockTime(index) - (lead.recoveryTicks + 1);
                if (remainingEffect > 0) {
                    float seconds = remainingEffect / 20.0f;
                    cdText = (remainingEffect <= 100) ? String.format("%.1f", seconds) : String.valueOf((int) Math.ceil(seconds));
                    textColor = 0xFF00E6FF;
                }
            }
        } else if (currentCd > 0) {
            // クールダウン中（白色）
            float seconds = currentCd / 20.0f;
            cdText = (currentCd <= 100) ? String.format("%.1f", seconds) : String.valueOf((int) Math.ceil(seconds));
        }

        if (!cdText.isEmpty()) {
            gui.pose().pushPose();
            // 文字をスロットの中央に配置
            float textScale = 0.8f; // インベントリのスロットは小さいのでスケールを絞る
            gui.pose().translate(x + (size / 2.0f), y + (size / 2.0f) - (font.lineHeight * textScale / 2.0f), 250);
            gui.pose().scale(textScale, textScale, textScale);
            gui.drawCenteredString(font, cdText, 0, 0, textColor);
            gui.pose().popPose();
        }
    }
    private static void drawSlotWithHighlight(GuiGraphics gui, int x, int y, int mx, int my) {
        gui.blit(INVENTORY_LOCATION, x, y, 7, 7, 18, 18);
        if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
            gui.fill(x, y, x + 18, y + 18, 0x80FFFFFF);
        }
    }
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen inv)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 型を明示的に HunterTransformation に指定
        mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
            if (!cap.isActive()) return;

            boolean bookVisible = inv.getRecipeBookComponent().isVisible();
            int xOffset = bookVisible ? 176 + 4 : -28;
            double mx = event.getMouseX();
            double my = event.getMouseY();
            int slotX = inv.getGuiLeft() + xOffset - 1;
            int top = inv.getGuiTop();

            for (int i = 0; i < 3; i++) {
                if (isHovering(mx, my, slotX, top + SKILL_START_Y + (i * 20) - 1, 18, 18)) {
                    if (isSkillOnCooldown(cap, i)) return; // ロック
                    openSelectionScreen(i);
                    event.setCanceled(true);
                    return;
                }
            }

            if (isHovering(mx, my, slotX, top + DODGE_Y - 1, 18, 18)) {
                if (isSkillOnCooldown(cap, 3)) return; // ロック
                openSelectionScreen(3);
                event.setCanceled(true);
            }
        });
    }

    // 第1引数を HunterTransformation に修正
    private static boolean isSkillOnCooldown(HunterTransformation cap, int slotIndex) {
        BaseIdentity identity = cap.getIdentity();
        if (identity == null) return false;
        // BaseIdentity の public メソッドである getCooldown と isEffectActive を呼び出す
        return identity.getCooldown(slotIndex) > 0 || identity.isEffectActive(slotIndex);
    }

    private static void openSelectionScreen(int slotIndex) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        WeaponCategory currentWeapon = WeaponCategory.NONE;
        var cap = mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).orElse(null);
        if (cap != null) {
            currentWeapon = com.mimic.monstermod.weapon.WeaponCategoryUtil.getCategory(cap.getWeaponSlot());
        }
        mc.setScreen(new SkillSelectionScreen(slotIndex, currentWeapon));
    }

    private static boolean isHovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}