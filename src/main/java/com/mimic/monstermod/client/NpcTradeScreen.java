package com.mimic.monstermod.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_ExecuteTradePacket;
import com.mimic.monstermod.npc.NpcTrade;
import com.mimic.monstermod.npc.NpcTradeSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * プレイヤー向けの交渉画面。
 *
 * バニラの村人GUIは「入力2・出力1」しか扱えないため独自に用意している。
 * 1行が1件の交渉で、入力アイテム群 → 出力アイテム群 を並べて表示する。
 *
 * 回数を使い切った交渉はバニラと同じように赤い×を出して選べなくする。
 * 残り回数の表示は交渉ごとの設定(showUses)に従う。
 */
public class NpcTradeScreen extends Screen {

    private static final int ROW_H = 26;
    private static final int PANEL_W = 320;
    private static final int SLOT = 18;

    private final int npcEntityId;
    private final String npcName;
    private NpcTradeSet tradeSet;

    private int scroll = 0;

    public NpcTradeScreen(int npcEntityId, String npcName, NpcTradeSet set) {
        super(Component.literal("交渉"));
        this.npcEntityId = npcEntityId;
        this.npcName = npcName;
        this.tradeSet = set;
    }

    /** サーバーで交換が成立した後、最新の在庫で表示を更新する */
    public void updateTrades(NpcTradeSet set) {
        this.tradeSet = set;
    }

    public int getNpcEntityId() { return npcEntityId; }

    private int rowsVisible() {
        return Math.max(1, (this.height - 90) / ROW_H);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, tradeSet.getTrades().size() - rowsVisible());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int idx = rowAt(mouseX, mouseY);
            if (idx >= 0) {
                NpcTrade t = tradeSet.getTrades().get(idx);
                if (!t.isSoldOut()) {
                    // 成否の判定と在庫の減算はサーバーが行う(クライアントを信用しない)
                    ModMessages.sendToServer(new C2S_ExecuteTradePacket(npcEntityId, idx));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 座標からどの交渉行かを求める。行外なら -1 */
    private int rowAt(double mouseX, double mouseY) {
        int left = this.width / 2 - PANEL_W / 2;
        int top = 44;
        if (mouseX < left || mouseX > left + PANEL_W) return -1;
        int rel = (int) ((mouseY - top) / ROW_H);
        if (rel < 0 || rel >= rowsVisible()) return -1;
        int idx = scroll + rel;
        return idx < tradeSet.getTrades().size() ? idx : -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int left = this.width / 2 - PANEL_W / 2;
        int top = 44;

        g.drawCenteredString(this.font, npcName + " との交渉", this.width / 2, 20, 0xFFFFFF);

        List<NpcTrade> trades = tradeSet.getTrades();
        if (trades.isEmpty()) {
            g.drawCenteredString(this.font, "交渉できるものはありません", this.width / 2, top + 10, 0xA0A0A0);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        int shown = Math.min(rowsVisible(), trades.size() - scroll);
        ItemStack hoveredStack = ItemStack.EMPTY; // ツールチップは全行を描いた後に出す(他の行に隠されないように)

        for (int i = 0; i < shown; i++) {
            int idx = scroll + i;
            NpcTrade t = trades.get(idx);
            int y = top + i * ROW_H;
            boolean hovered = rowAt(mouseX, mouseY) == idx;

            g.fill(left, y, left + PANEL_W, y + ROW_H - 2, hovered ? 0x80303030 : 0x80101010);

            int x = left + 4;
            x = drawStacks(g, t.getInputs(), x, y + 4, mouseX, mouseY);
            g.drawString(this.font, "→", x + 2, y + 9, 0xFFFFFF, false);
            x += 14;
            drawStacks(g, t.getOutputs(), x, y + 4, mouseX, mouseY);

            if (hoveredStack.isEmpty()) hoveredStack = findHovered(t, left, y, mouseX, mouseY);

            // 右端に残り回数、または売り切れの赤い×(バニラの村人GUIと同じ意味)
            if (t.isSoldOut()) {
                drawRedCross(g, left + PANEL_W - 20, y + 5, 10);
            } else if (t.isShowUses() && !t.isUnlimited()) {
                String label = "残り " + t.remaining() + " 回";
                g.drawString(this.font, label,
                        left + PANEL_W - 8 - this.font.width(label), y + 9, 0xC0C0C0, false);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (!hoveredStack.isEmpty()) g.renderTooltip(this.font, hoveredStack, mouseX, mouseY);

        g.drawCenteredString(this.font, "クリックで交換 / ホイールでスクロール",
                this.width / 2, this.height - 16, 0xA0A0A0);
    }

    /** アイテム列を並べて描画し、次のX座標を返す */
    private int drawStacks(GuiGraphics g, List<ItemStack> stacks, int x, int y, int mouseX, int mouseY) {
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            g.renderItem(s, x, y);
            g.renderItemDecorations(this.font, s, x, y);
            x += SLOT + 2;
        }
        return x;
    }

    /** その行のどのアイテムにマウスが乗っているかを、描画と同じ配置計算で求める */
    private ItemStack findHovered(NpcTrade t, int left, int y, int mouseX, int mouseY) {
        int sy = y + 4;
        if (mouseY < sy || mouseY >= sy + SLOT) return ItemStack.EMPTY;

        int x = left + 4;
        for (ItemStack s : t.getInputs()) {
            if (s.isEmpty()) continue;
            if (mouseX >= x && mouseX < x + SLOT) return s;
            x += SLOT + 2;
        }
        x += 14; // 「→」のぶん
        for (ItemStack s : t.getOutputs()) {
            if (s.isEmpty()) continue;
            if (mouseX >= x && mouseX < x + SLOT) return s;
            x += SLOT + 2;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 売り切れを示す赤い×を描く。
     * フォントに × のグリフが無い環境でも同じ見た目になるよう、点を並べて自前で描く。
     */
    private void drawRedCross(GuiGraphics g, int x, int y, int size) {
        final int color = 0xFFFF5555;
        for (int i = 0; i < size; i++) {
            g.fill(x + i, y + i, x + i + 2, y + i + 2, color);
            g.fill(x + size - 1 - i, y + i, x + size + 1 - i, y + i + 2, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
