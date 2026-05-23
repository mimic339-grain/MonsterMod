package com.mimic.monstermod.gui;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SUpdatePlayerVisualPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisualConfigScreen extends Screen {
    private final Screen lastScreen;
    private boolean show;
    private float r, g, b, thickness; // ★ thickness追加

    public VisualConfigScreen(Screen lastScreen) {
        super(Component.literal("Monster Mod 描画設定"));
        this.lastScreen = lastScreen;

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
                this.show = cap.hasState(IPlayerData.STATE_SHOW_SKILL_LEAD);
                this.r = cap.getLeadR();
                this.g = cap.getLeadG();
                this.b = cap.getLeadB();
                this.thickness = cap.getLeadThickness(); // ★ 取得
            });
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // ON/OFF 切り替え
        this.addRenderableWidget(Button.builder(Component.literal("当たり判定表示: " + (show ? "ON" : "OFF")), (btn) -> {
            show = !show;
            btn.setMessage(Component.literal("当たり判定表示: " + (show ? "ON" : "OFF")));
        }).bounds(centerX - 100, 45, 200, 20).build());

        // カラー選択（5色並べる）
        int bw = 38; int space = 2;
        int startX = centerX - ((bw * 5 + space * 4) / 2);
        this.addRenderableWidget(Button.builder(Component.literal("赤"), (btn) -> { r=1f; g=0f; b=0f; }).bounds(startX, 70, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("緑"), (btn) -> { r=0f; g=1f; b=0f; }).bounds(startX + (bw+space), 70, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("青"), (btn) -> { r=0f; g=0f; b=1f; }).bounds(startX + (bw+space)*2, 70, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("白"), (btn) -> { r=1f; g=1f; b=1f; }).bounds(startX + (bw+space)*3, 70, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("黒"), (btn) -> { r=0f; g=0f; b=0f; }).bounds(startX + (bw+space)*4, 70, bw, 20).build());

        // ★ 太さ調整スライダー (1.0 ～ 10.0)
        this.addRenderableWidget(new net.minecraft.client.gui.components.AbstractSliderButton(centerX - 100, 95, 200, 20, Component.literal("線の太さ: " + String.format("%.1f", thickness)), (double)((thickness - 1.0f) / 9.0f)) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("線の太さ: " + String.format("%.1f", thickness))); }
            @Override protected void applyValue() { thickness = 1.0f + (float)(this.value * 9.0f); }
        });

        // 完了ボタン
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), (btn) -> {
            if (Minecraft.getInstance().getConnection() != null) {
                ModMessages.sendToServer(new C2SUpdatePlayerVisualPacket(show, r, g, b, thickness));
            }
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(centerX - 100, 160, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        int color = 0xFF000000 | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        pGuiGraphics.fill(this.width / 2 - 15, 125, this.width / 2 + 15, 150, color);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
}