package com.mimic.monstermod.client;

import com.mimic.monstermod.npc.NpcTrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * NPC設定の2ページ目。交渉(取引)内容を編集する画面。
 *
 * 【アイテムの指定方法】要望通り2通りに対応する:
 *   1. アイテムIDを打ち込む(Tabで補完。ブロックもアイテムも同じ一覧から選べる)
 *   2. 「手持ち」ボタン → 手に持っているアイテムを取り込む(クリエイティブタブから掴めばよい)
 *
 * 入力・出力ともに複数追加できるので
 * 「エメラルド20 + ダイヤ20 → 木64 + 石64 + 鉄64 + 金64」のような交渉も作れる。
 *
 * 【保存の流れ】このリストは {@link NpcEditorScreen} が持つ List をその場で書き換える。
 * 実際のサーバー保存は親画面の「保存して閉じる」(C2S_SaveNpcSettingsPacket)でまとめて行う。
 */
public class TradeEditorScreen extends Screen {

    private static final int PANEL_W = 340;
    private static final int SLOT = 18;
    private static final int MAX_SUGGEST = 6;

    // アイテム一覧の描画レイアウト(1行8個・最大2段)
    private static final int LIST_COLS = 8;
    private static final int LIST_ROWS = 2;
    private static final int LIST_STEP = 20;
    private static final int INPUT_Y = 84;
    private static final int OUTPUT_Y = INPUT_Y + LIST_ROWS * LIST_STEP + 4;

    private final Screen parent;
    private final List<NpcTrade> trades; // 親画面のリストを直接編集する
    private int index = 0;

    private EditBox itemBox, countBox, maxUsesBox;
    private Button showUsesBtn;
    private boolean addingInput = true; // 「追加」の追加先が入力側か出力側か

    private final List<String> suggestions = new ArrayList<>();
    private int suggestIndex = 0;

    private ItemStack pendingPick;               // 選択画面で選ばれた直後のアイテム
    private ItemStack pickedStack = ItemStack.EMPTY; // ID欄の内容と一致する間はこれを使う(NBTを保つため)

    public TradeEditorScreen(Screen parent, List<NpcTrade> trades) {
        super(Component.literal("交渉設定"));
        this.parent = parent;
        this.trades = trades;
        if (trades.isEmpty()) trades.add(new NpcTrade(0, true));
    }

    private NpcTrade current() {
        if (index >= trades.size()) index = trades.size() - 1;
        return trades.get(index);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int x = cx - PANEL_W / 2;
        int y = 34;

        itemBox = new EditBox(this.font, x + 60, y, 180, 18, Component.empty());
        itemBox.setMaxLength(128);
        itemBox.setResponder(v -> updateSuggestions());
        addRenderableWidget(itemBox);

        countBox = new EditBox(this.font, x + 244, y, 40, 18, Component.empty());
        countBox.setValue("1");
        countBox.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(countBox);

        addRenderableWidget(Button.builder(Component.literal("タブ"), b -> openPicker())
                .bounds(x + 290, y, 50, 18).build());
        y += 24;

        // 追加先(入力/出力)を切り替えてから「追加」を押す
        addRenderableWidget(Button.builder(Component.literal("追加先: 入力"), b -> {
            addingInput = !addingInput;
            b.setMessage(Component.literal("追加先: " + (addingInput ? "入力" : "出力")));
        }).bounds(x, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("追加"), b -> addItem())
                .bounds(x + 104, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("入力を消す"), b -> current().getInputs().clear())
                .bounds(x + 168, y, 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal("出力を消す"), b -> current().getOutputs().clear())
                .bounds(x + 256, y, 84, 20).build());

        // この下(INPUT_Y 〜)に入力/出力の中身をアイコンで描画する。
        // 1行8個で最大2段ずつ確保しているので、片側16個まで隠れずに見える
        y += 100;

        maxUsesBox = new EditBox(this.font, x + 110, y, 50, 18, Component.empty());
        maxUsesBox.setValue(String.valueOf(current().getMaxUses()));
        maxUsesBox.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(maxUsesBox);

        showUsesBtn = Button.builder(showUsesLabel(), b -> {
            current().setShowUses(!current().isShowUses());
            b.setMessage(showUsesLabel());
        }).bounds(x + 168, y, 172, 18).build();
        addRenderableWidget(showUsesBtn);
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("< 前"), b -> move(-1)).bounds(x, y, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("次 >"), b -> move(1)).bounds(x + 54, y, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("交渉を追加"), b -> addTrade()).bounds(x + 110, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("この交渉を削除"), b -> removeTrade()).bounds(x + 216, y, 124, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("< NPC設定へ戻る"), b -> onClose())
                .bounds(cx - 100, y, 200, 20).build());

        consumePendingPick(); // アイテム選択画面から戻ってきた場合の反映
    }

    private Component showUsesLabel() {
        return Component.literal("残り回数を見せる: " + (current().isShowUses() ? "ON" : "OFF"));
    }

    /** 入力欄のIDと個数からアイテムを作って、入力側/出力側へ追加する */
    private void addItem() {
        String id = itemBox.getValue().trim();
        if (id.isEmpty()) return;

        Item item;
        try {
            item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        } catch (Exception e) {
            return; // IDの書式が不正
        }
        if (item == null) return;

        // 選択画面で選んだものと同じアイテムなら、そのStack(NBT付き)をそのまま使う
        ItemStack stack = (!pickedStack.isEmpty() && pickedStack.is(item))
                ? pickedStack.copy() : new ItemStack(item);
        stack.setCount(readCount());

        (addingInput ? current().getInputs() : current().getOutputs()).add(stack);
    }

    /**
     * クリエイティブのインベントリと同じ感覚でアイテムを選ぶ画面を開く。
     * 選んだアイテムはID欄にセットされるので、個数を決めて「追加」を押せばよい。
     * 選択画面の実装: {@link ItemPickerScreen}
     */
    private void openPicker() {
        pendingPick = null;
        Minecraft.getInstance().setScreen(new ItemPickerScreen(this, stack -> pendingPick = stack));
    }

    /**
     * 選択画面から戻ってきたときに、選んだアイテムをID欄へ反映する。
     * init() は画面が復帰した時点で走るため、ここで受け取るのが確実。
     */
    private void consumePendingPick() {
        if (pendingPick == null || pendingPick.isEmpty()) return;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(pendingPick.getItem());
        if (id != null) itemBox.setValue(id.toString());
        pickedStack = pendingPick.copy(); // NBT付き(エンチャント本など)はこちらを優先して使う
        pendingPick = null;
    }

    private int readCount() {
        try {
            return Math.max(1, Integer.parseInt(countBox.getValue()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** 登録済みアイテム一覧から候補を作る(ブロックもアイテムとして登録されているので同じ一覧に出る) */
    private void updateSuggestions() {
        suggestions.clear();
        suggestIndex = 0;
        String v = itemBox.getValue().toLowerCase(Locale.ROOT);
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            String s = id.toString();
            if (s.toLowerCase(Locale.ROOT).contains(v)) suggestions.add(s);
            if (suggestions.size() >= 200) break;
        }
        suggestions.sort(String::compareTo);
    }

    private boolean hasSuggestions() {
        return itemBox != null && itemBox.isFocused() && !suggestions.isEmpty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258 && hasSuggestions()) { // Tabで補完
            itemBox.setValue(suggestions.get(suggestIndex % suggestions.size()));
            suggestIndex++;
            return true;
        }
        if (hasSuggestions() && (keyCode == 264 || keyCode == 265)) { // ↓↑で候補を送る
            suggestIndex = Math.max(0, suggestIndex + (keyCode == 264 ? 1 : -1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 入力欄の総回数をデータ側へ書き戻す(ページ移動・閉じる前に必ず呼ぶ) */
    private void syncMaxUses() {
        String v = maxUsesBox.getValue();
        try {
            current().setMaxUses(v.isEmpty() ? 0 : Integer.parseInt(v));
        } catch (NumberFormatException ignored) { }
    }

    private void refreshFields() {
        maxUsesBox.setValue(String.valueOf(current().getMaxUses()));
        showUsesBtn.setMessage(showUsesLabel());
    }

    private void move(int d) {
        syncMaxUses();
        int next = index + d;
        if (next < 0 || next >= trades.size()) return;
        index = next;
        refreshFields();
    }

    private void addTrade() {
        syncMaxUses();
        trades.add(index + 1, new NpcTrade(0, true));
        index++;
        refreshFields();
    }

    private void removeTrade() {
        syncMaxUses();
        if (trades.size() <= 1) {
            // 最後の1件は消さず空にする(常に1ページ表示できるようにするため)
            trades.set(0, new NpcTrade(0, true));
            index = 0;
            refreshFields();
            return;
        }
        trades.remove(index);
        if (index >= trades.size()) index = trades.size() - 1;
        refreshFields();
    }

    @Override
    public void onClose() {
        syncMaxUses();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int x = cx - PANEL_W / 2;

        g.drawCenteredString(this.font, "交渉設定  (" + (index + 1) + " / " + trades.size() + ")", cx, 14, 0xFFFFFF);
        g.drawString(this.font, "アイテム", x, 39, 0xC0C0C0, false);

        g.drawString(this.font, "入力", x, INPUT_Y + 5, 0xFFC0C0, false);
        g.drawString(this.font, "出力", x, OUTPUT_Y + 5, 0xC0FFC0, false);
        g.drawString(this.font, "総回数(0=無制限)", x, 34 + 24 + 100 + 5, 0xC0C0C0, false);

        super.render(g, mouseX, mouseY, partialTick);

        // 中身は widget より後ろに描かないとツールチップが隠れるのでここで描く
        ItemStack hovered = drawStacks(g, current().getInputs(), x + 30, INPUT_Y, mouseX, mouseY);
        if (hovered.isEmpty()) {
            hovered = drawStacks(g, current().getOutputs(), x + 30, OUTPUT_Y, mouseX, mouseY);
        } else {
            drawStacks(g, current().getOutputs(), x + 30, OUTPUT_Y, mouseX, mouseY);
        }
        if (!hovered.isEmpty()) g.renderTooltip(this.font, hovered, mouseX, mouseY);

        if (hasSuggestions()) {
            int sx = itemBox.getX();
            int sy = itemBox.getY() + 19;
            int n = Math.min(MAX_SUGGEST, suggestions.size());
            g.fill(sx, sy, sx + itemBox.getWidth(), sy + n * 11 + 2, 0xE0000000);
            for (int i = 0; i < n; i++) {
                int idx = (suggestIndex + i) % suggestions.size();
                g.drawString(this.font, suggestions.get(idx), sx + 3, sy + 2 + i * 11,
                        i == 0 ? 0xFFFFA0 : 0xB0B0B0, false);
            }
        }

        g.drawCenteredString(this.font, "「タブ」でクリエイティブの一覧から選択 / 直接入力ならTabで補完",
                cx, this.height - 12, 0xA0A0A0);
    }

    /**
     * アイテム一覧を1行 LIST_COLS 個で折り返して描く。
     * 1行に並べ続けると画面からはみ出して見えなくなるため、必ず折り返す。
     * 入りきらないぶんは最後の枠に「+N」で出す。
     */
    private ItemStack drawStacks(GuiGraphics g, List<ItemStack> stacks, int baseX, int baseY,
                                 int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        int max = LIST_COLS * LIST_ROWS;
        int shown = Math.min(stacks.size(), max);

        for (int i = 0; i < shown; i++) {
            ItemStack s = stacks.get(i);
            if (s.isEmpty()) continue;

            int x = baseX + (i % LIST_COLS) * LIST_STEP;
            int y = baseY + (i / LIST_COLS) * LIST_STEP;

            g.fill(x - 1, y - 1, x + 17, y + 17, 0x40FFFFFF); // 枠(位置を分かりやすくするだけ)
            g.renderItem(s, x, y);
            g.renderItemDecorations(this.font, s, x, y);

            if (i == max - 1 && stacks.size() > max) {
                g.drawString(this.font, "+" + (stacks.size() - max), x + 1, y - 5, 0xFFFF55, true);
            }
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) hovered = s;
        }
        return hovered;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
