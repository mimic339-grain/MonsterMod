package com.mimic.monstermod.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_ExecuteTradePacket;
import com.mimic.monstermod.npc.NpcTrade;
import com.mimic.monstermod.npc.NpcTradeSet;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * プレイヤー向けの交渉画面。
 *
 * 見た目はバニラの村人取引画面(MerchantScreen)と同じになるようにしてある。
 * 背景・矢印・売り切れの赤い×・スクロールバーは全てバニラのテクスチャ
 * (textures/gui/container/villager2.png)から同じ座標で切り出しており、
 * ボタン位置・アイテム位置・ラベル位置もバニラと同じ値を使っている。
 *
 * 【バニラの MerchantScreen をそのまま使わない理由】
 * バニラの MerchantOffer は「入力2個・出力1個」しか表現できない。
 * こちらは入力・出力とも複数(例: エメラルド20+ダイヤ20 → 木64+石64+鉄64+金64)を
 * 扱うため、同じ見た目を保ったまま右パネルのスロットを増やす形で作り直している。
 *
 * 【操作】バニラと同じ
 *   左の一覧をクリック  → その交渉を選ぶ(右パネルに内容が出る)
 *   右の結果スロットをクリック → 交換する
 */
public class NpcTradeScreen extends Screen {

    private static final ResourceLocation VILLAGER = new ResourceLocation("textures/gui/container/villager2.png");
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int IMAGE_W = 276, IMAGE_H = 166;

    private static final int OFFER_BUTTONS = 7;
    private static final int TRADE_BTN_W = 88, TRADE_BTN_H = 20;

    // 右パネルのスロット座標(バニラの MerchantMenu と同じ)
    private static final int SELL1_X = 136, SELL2_X = 162, BUY_X = 220, ROW_Y = 37;
    private static final int SLOT_STEP_X = 26, SLOT_STEP_Y = 20;
    private static final int MAX_SIDE_SLOTS = 4; // 右パネルに並べられる個数(2列×2段)

    // 空スロットの絵は背景テクスチャの支払いスロット部分をそのまま流用する
    private static final float SLOT_U = SELL1_X - 1, SLOT_V = ROW_Y - 1;

    private final int npcEntityId;
    private final String npcName;
    private NpcTradeSet tradeSet;

    private final Button[] offerButtons = new Button[OFFER_BUTTONS];
    private int leftPos, topPos;
    private int selected = 0;
    private int scrollOff = 0;
    private boolean dragging;

    public NpcTradeScreen(int npcEntityId, String npcName, NpcTradeSet set) {
        super(Component.literal(npcName));
        this.npcEntityId = npcEntityId;
        this.npcName = npcName;
        this.tradeSet = set;
    }

    /** サーバーで交換が成立した後、最新の在庫で表示を更新する */
    public void updateTrades(NpcTradeSet set) {
        this.tradeSet = set;
        if (selected >= set.getTrades().size()) selected = Math.max(0, set.getTrades().size() - 1);
    }

    public int getNpcEntityId() { return npcEntityId; }

    private List<NpcTrade> trades() { return tradeSet.getTrades(); }

    @Override
    protected void init() {
        leftPos = (this.width - IMAGE_W) / 2;
        topPos = (this.height - IMAGE_H) / 2;

        int y = topPos + 16 + 2;
        for (int i = 0; i < OFFER_BUTTONS; i++) {
            final int index = i;
            offerButtons[i] = addRenderableWidget(
                    Button.builder(Component.empty(), b -> selected = index + scrollOff)
                            .bounds(leftPos + 5, y, TRADE_BTN_W, TRADE_BTN_H).build());
            offerButtons[i].visible = false;
            y += 20;
        }
    }

    // ---------------- 入力 ----------------

    private boolean canScroll() { return trades().size() > OFFER_BUTTONS; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (canScroll()) {
            scrollOff = Mth.clamp((int) (scrollOff - delta), 0, trades().size() - OFFER_BUTTONS);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = false;

        // スクロールバーのつまみ(バニラと同じ判定範囲)
        if (canScroll() && mouseX > leftPos + 94 && mouseX < leftPos + 94 + 6
                && mouseY > topPos + 18 && mouseY <= topPos + 18 + 139 + 1) {
            dragging = true;
            return true;
        }

        // 結果スロットをクリックしたら交換する(バニラで結果を取り出すのと同じ操作)
        if (button == 0 && isOverResultArea(mouseX, mouseY)) {
            NpcTrade t = selectedTrade();
            if (t != null && !t.isSoldOut()) {
                // 成否の判定・所持品の消費・在庫の減算は全てサーバーが行う
                ModMessages.sendToServer(new C2S_ExecuteTradePacket(npcEntityId, selected));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && canScroll()) {
            int top = topPos + 18;
            int bottom = top + 139;
            int max = trades().size() - OFFER_BUTTONS;
            float f = ((float) mouseY - (float) top - 13.5F) / ((float) (bottom - top) - 27.0F);
            scrollOff = Mth.clamp((int) (f * (float) max + 0.5F), 0, max);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private boolean isOverResultArea(double mouseX, double mouseY) {
        return mouseX >= leftPos + BUY_X - 1 && mouseX < leftPos + BUY_X + SLOT_STEP_X + 17
                && mouseY >= topPos + ROW_Y - 1 && mouseY < topPos + ROW_Y + SLOT_STEP_Y + 17;
    }

    private NpcTrade selectedTrade() {
        List<NpcTrade> list = trades();
        return (selected >= 0 && selected < list.size()) ? list.get(selected) : null;
    }

    // ---------------- 描画 ----------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // 背景(バニラの村人GUIそのもの。プレイヤーインベントリの枠も含まれている)
        g.blit(VILLAGER, leftPos, topPos, 0, 0.0F, 0.0F, IMAGE_W, IMAGE_H, TEX_W, TEX_H);

        List<NpcTrade> list = trades();
        for (int i = 0; i < OFFER_BUTTONS; i++) {
            offerButtons[i].visible = i + scrollOff < list.size();
        }

        super.render(g, mouseX, mouseY, partialTick); // 取引ボタン(バニラと同じ見た目)

        drawLabels(g);
        drawScroller(g, list);

        ItemStack hovered = drawOfferRows(g, list, mouseX, mouseY);
        ItemStack detailHover = drawDetailPanel(g, mouseX, mouseY);
        if (hovered.isEmpty()) hovered = detailHover;

        ItemStack invHover = drawPlayerInventory(g, mouseX, mouseY);
        if (hovered.isEmpty()) hovered = invHover;

        RenderSystem.enableDepthTest();
        if (!hovered.isEmpty()) g.renderTooltip(this.font, hovered, mouseX, mouseY);
    }

    /** ラベル位置・色はバニラの MerchantScreen#renderLabels と同じ */
    private void drawLabels(GuiGraphics g) {
        final int COLOR = 4210752;
        Component title = Component.literal(npcName);
        g.drawString(this.font, title,
                leftPos + 49 + IMAGE_W / 2 - this.font.width(title) / 2, topPos + 6, COLOR, false);

        Component trades = Component.translatable("merchant.trades");
        g.drawString(this.font, trades,
                leftPos + 5 - this.font.width(trades) / 2 + 48, topPos + 6, COLOR, false);

        Component inv = Component.translatable("container.inventory");
        g.drawString(this.font, inv, leftPos + 107, topPos + IMAGE_H - 94, COLOR, false);
    }

    /** スクロールバー。座標・UVはバニラと同じ */
    private void drawScroller(GuiGraphics g, List<NpcTrade> list) {
        int i = list.size() + 1 - OFFER_BUTTONS;
        if (i > 1) {
            int j = 139 - (27 + (i - 1) * 139 / i);
            int k = 1 + j / i + 139 / i;
            int pos = Math.min(113, scrollOff * k);
            if (scrollOff == i - 1) pos = 113;
            g.blit(VILLAGER, leftPos + 94, topPos + 18 + pos, 0, 0.0F, 199.0F, 6, 27, TEX_W, TEX_H);
        } else {
            g.blit(VILLAGER, leftPos + 94, topPos + 18, 0, 6.0F, 199.0F, 6, 27, TEX_W, TEX_H);
        }
    }

    /**
     * 左側の取引一覧。入力2つ・矢印・出力1つという並びはバニラと同じ。
     * 3つ目以降の入力や2つ目以降の出力がある場合は「+」印を出し、詳細は右パネルで見せる。
     */
    private ItemStack drawOfferRows(GuiGraphics g, List<NpcTrade> list, int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        int rowY = topPos + 16 + 1;
        int x = leftPos + 5;

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 100.0F);

        for (int i = 0; i < OFFER_BUTTONS; i++) {
            int idx = i + scrollOff;
            if (idx >= list.size()) break;

            NpcTrade t = list.get(idx);
            int y = rowY + i * 20 + 2;

            hovered = pickAndDraw(g, t.getInputs(), 0, x + 5, y, mouseX, mouseY, hovered);
            hovered = pickAndDraw(g, t.getInputs(), 1, x + 35, y, mouseX, mouseY, hovered);

            // 矢印。売り切れなら赤い×(バニラと同じUV)
            g.blit(VILLAGER, x + 35 + 20, y + 3, 0,
                    t.isSoldOut() ? 25.0F : 15.0F, 171.0F, 10, 9, TEX_W, TEX_H);

            hovered = pickAndDraw(g, t.getOutputs(), 0, x + 68, y, mouseX, mouseY, hovered);

            if (t.getInputs().size() > 2) g.drawString(this.font, "+", x + 35 + 12, y - 1, 0xFFFF55, false);
            if (t.getOutputs().size() > 1) g.drawString(this.font, "+", x + 68 + 12, y - 1, 0xFFFF55, false);
        }

        g.pose().popPose();
        return hovered;
    }

    /** リストのn番目を指定位置に描く。マウスが乗っていればそのアイテムを返す */
    private ItemStack pickAndDraw(GuiGraphics g, List<ItemStack> list, int n, int x, int y,
                                  int mouseX, int mouseY, ItemStack current) {
        if (n >= list.size()) return current;
        ItemStack stack = list.get(n);
        if (stack.isEmpty()) return current;

        g.renderFakeItem(stack, x, y);
        g.renderItemDecorations(this.font, stack, x, y);

        if (current.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            return stack;
        }
        return current;
    }

    /**
     * 右パネル。選んだ交渉の中身を全部見せる。
     * バニラでは支払い2枠+結果1枠だが、複数入力・複数出力に対応するため
     * 同じ絵のスロットを2列×2段まで並べる。
     */
    private ItemStack drawDetailPanel(GuiGraphics g, int mouseX, int mouseY) {
        NpcTrade t = selectedTrade();
        if (t == null) return ItemStack.EMPTY;

        ItemStack hovered = ItemStack.EMPTY;
        hovered = drawSlotGrid(g, t.getInputs(), SELL1_X, SELL2_X, mouseX, mouseY, hovered);
        hovered = drawSlotGrid(g, t.getOutputs(), BUY_X, BUY_X + SLOT_STEP_X, mouseX, mouseY, hovered);

        // 売り切れなら中央の矢印を赤い×で塗り潰す(バニラと同じ位置・同じ絵)
        if (t.isSoldOut()) {
            g.blit(VILLAGER, leftPos + 83 + 99, topPos + 35, 0, 311.0F, 0.0F, 28, 21, TEX_W, TEX_H);
        } else if (t.isShowUses() && !t.isUnlimited()) {
            // 矢印の真上。スロットが2段になっても被らない位置
            String label = "残り " + t.remaining() + " 回";
            g.drawString(this.font, label, leftPos + 196 - this.font.width(label) / 2,
                    topPos + 26, 4210752, false);
        }
        return hovered;
    }

    /** 2列×2段のスロットを描いてアイテムを並べる。スロットの絵は背景から切り出して使う */
    private ItemStack drawSlotGrid(GuiGraphics g, List<ItemStack> items, int col1X, int col2X,
                                   int mouseX, int mouseY, ItemStack current) {
        int shown = Math.min(items.size(), MAX_SIDE_SLOTS);
        for (int i = 0; i < MAX_SIDE_SLOTS; i++) {
            int localX = (i % 2 == 0) ? col1X : col2X;
            int localY = ROW_Y + (i / 2) * SLOT_STEP_Y;
            int sx = leftPos + localX;
            int sy = topPos + localY;

            // 背景テクスチャに元から枠が描かれている位置(支払い2枠と結果1枠)以外は、
            // 同じ枠の絵を背景からコピーして足す
            boolean vanillaSlot = localY == ROW_Y
                    && (localX == SELL1_X || localX == SELL2_X || localX == BUY_X);
            if (!vanillaSlot) {
                g.blit(VILLAGER, sx - 1, sy - 1, 0, SLOT_U, SLOT_V, 18, 18, TEX_W, TEX_H);
            }

            if (i >= shown) continue;
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            g.renderFakeItem(stack, sx, sy);
            g.renderItemDecorations(this.font, stack, sx, sy);
            if (current.isEmpty() && mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                current = stack;
            }
        }
        return current;
    }

    /**
     * プレイヤーの所持品を背景の枠に合わせて描く。
     * バニラの MerchantMenu と同じスロット座標を使っているので位置はぴったり合う。
     * (この画面では並べ替えはしないため表示のみ)
     */
    private ItemStack drawPlayerInventory(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;

        Inventory inv = mc.player.getInventory();
        ItemStack hovered = ItemStack.EMPTY;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                hovered = drawInvSlot(g, inv.getItem(col + row * 9 + 9),
                        leftPos + 108 + col * 18, topPos + 84 + row * 18, mouseX, mouseY, hovered);
            }
        }
        for (int col = 0; col < 9; col++) {
            hovered = drawInvSlot(g, inv.getItem(col),
                    leftPos + 108 + col * 18, topPos + 142, mouseX, mouseY, hovered);
        }
        return hovered;
    }

    private ItemStack drawInvSlot(GuiGraphics g, ItemStack stack, int x, int y,
                                  int mouseX, int mouseY, ItemStack current) {
        if (stack.isEmpty()) return current;
        g.renderItem(stack, x, y);
        g.renderItemDecorations(this.font, stack, x, y);
        if (current.isEmpty() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            return stack;
        }
        return current;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
