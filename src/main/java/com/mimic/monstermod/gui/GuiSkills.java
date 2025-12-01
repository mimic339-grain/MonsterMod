package com.mimic.monstermod.gui;

import com.mimic.monstermod.impl.MonsterKeyBindings;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IEntityData;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class GuiSkills {

    public static final IGuiOverlay OVERLAY = GuiSkills::drawHUD;
    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean shouldDisplayBar() {
        return !mc.options.hideGui;
    }

    public static void drawHUD(ForgeGui gui, GuiGraphics guiGraphics, float pt, int width, int height) {
        if (!shouldDisplayBar()) return;

        Player player = mc.player;
        if (player == null) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    if (!trans.isTransformed()) return;

                    IEntityData entityData = CapabilityRegistry.getPlayerData(player);
                    if (entityData == null) return;

                    int w = mc.getWindow().getGuiScaledWidth();
                    int h = mc.getWindow().getGuiScaledHeight();

                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

                    // --- HUD 描画位置調整（ホットバー上に表示） ---
                    int guiWidth = 182;
                    int guiHeight = 22;
                    int hotbarHeight = 20; // ホットバーの高さ
                    int xPos = w - guiWidth - 4;
                    int yPos = h - guiHeight - hotbarHeight - 4;

                    // skill_gui.png の描画（テクスチャサイズ 256x256 に合わせる）
                    guiGraphics.blit(
                            new net.minecraft.resources.ResourceLocation("monstermod", "textures/gui/monster_skill_gui.png"),
                            xPos, yPos, 0, 0, guiWidth, guiHeight, 256, 256
                    );

                    // --- 使用キー & スキル名を自動取得して描画 ---
                    int mainScroll = entityData.getMainScroll();
                    String skillName = "Skill " + (mainScroll + 1); // 1～N表示
                    String keyName = MonsterKeyBindings.SKILL_KEYS.length > mainScroll ?
                            MonsterKeyBindings.SKILL_KEYS[mainScroll].getName() :
                            "unknown";

                    guiGraphics.drawString(mc.font,
                            ChatFormatting.BOLD + skillName,
                            xPos + 30, yPos + 6, 0xFFFFFF);

                    guiGraphics.drawString(mc.font,
                            "KEY: " + keyName,
                            xPos + 4, yPos + 6, 0xFFFFFF);

                    // --- widgets.png で DGKスクロールカー描画 ---
                    int sc = 20;
                    guiGraphics.blit(
                            new net.minecraft.resources.ResourceLocation("monstermod", "textures/gui/widgets.png"),
                            xPos - 1,
                            yPos - 1 + (entityData.getDGKScroll() * sc - sc),
                            0, 0, 24, 24, 256, 256
                    );

                    RenderSystem.depthMask(true);
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                });
    }
}
