package com.mimic.monstermod.client;

import com.mimic.monstermod.bomb.BombTiming;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetBombTimerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 設置ボムの時間を決める画面。
 *
 * 置いてから右クリックで開く。選んだ瞬間にカウントが始まり、以後は止められない
 * (解除キットを使うしかない)。
 * 長い時間を選ぶほど爆発半径が大きくなるので、
 * 「早く起爆したいか、大きく吹き飛ばしたいか」の選択になる。
 * どれくらい大きくなるかは選択肢に半径として出している。
 */
public class BombTimerScreen extends Screen {

    /** 選べる時間(秒) */
    private static final int[] CHOICES = { 10, 30, 60, 120, 300 };

    private final BlockPos pos;

    public BombTimerScreen(BlockPos pos) {
        super(Component.literal("起爆時間を決める"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - (CHOICES.length * 24) / 2;

        for (int seconds : CHOICES) {
            float radius = BombTiming.radiusForFuse(seconds * BombTiming.TICKS_PER_SECOND);
            String label = BombTiming.format(seconds * BombTiming.TICKS_PER_SECOND)
                    + "   （半径 " + String.format("%.1f", radius) + "）";

            final int sec = seconds;
            addRenderableWidget(Button.builder(Component.literal(label), b -> choose(sec))
                    .bounds(cx - 110, y, 220, 20).build());
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("やめる"), b -> onClose())
                .bounds(cx - 60, y + 6, 120, 20).build());
    }

    private void choose(int seconds) {
        ModMessages.sendToServer(new C2S_SetBombTimerPacket(pos, seconds));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, "起爆時間を決める", this.width / 2, 24, 0xFFFFFF);
        g.drawCenteredString(this.font, "長いほど爆発が大きい / 決めたら止められない",
                this.width / 2, 38, 0xFF8080);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
