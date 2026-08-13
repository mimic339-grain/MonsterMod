package com.mimic.monstermod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * アイテムを選ぶための画面。クリエイティブのインベントリと同じ感覚で使える。
 *
 * ・上部にクリエイティブタブのアイコンが並び、押すとそのタブの中身に切り替わる
 * ・検索欄あり(表示名でもID でも引ける。日本語名でも検索できる)
 * ・アイテムをクリックすると呼び出し元へそのアイテムを返して閉じる
 *
 * 呼び出し元: {@link TradeEditorScreen}（交渉の入力/出力アイテムの指定）
 *
 * 【クリエイティブタブから中身を取る理由】
 * 単純に登録済みアイテムを全部並べるとNBT付きのアイテム(エンチャント本・ポーションなど)が
 * 出てこない。クリエイティブタブの中身はNBT込みのItemStackなので、
 * クリエイティブ画面で掴めるものはそのまま指定できる。
 */
public class ItemPickerScreen extends Screen {

    private static final int COLS = 9;
    private static final int ROWS = 8;
    private static final int SLOT = 18;
    private static final int TAB_ALL = -1;

    private final Screen parent;
    private final Consumer<ItemStack> onPick;

    private final List<CreativeModeTab> tabs = new ArrayList<>();
    private int selectedTab = TAB_ALL;

    private EditBox searchBox;
    private final List<ItemStack> shown = new ArrayList<>();
    private int scroll = 0;

    // レイアウト(init で確定させる)
    private int gridLeft, gridTop, tabTop, tabRows;

    public ItemPickerScreen(Screen parent, Consumer<ItemStack> onPick) {
        super(Component.literal("アイテムを選ぶ"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        tabs.clear();
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            // 検索タブ・インベントリタブなどはアイテム一覧として意味がないので除く
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            if (tab.getDisplayItems().isEmpty()) continue;
            tabs.add(tab);
        }

        int cellCount = tabs.size() + 1; // 「すべて」のぶん
        tabRows = Math.max(1, (cellCount + COLS - 1) / COLS);

        tabTop = 30;
        int searchY = tabTop + tabRows * SLOT + 6;
        gridTop = searchY + 24;
        gridLeft = (this.width - COLS * SLOT) / 2;

        searchBox = new EditBox(this.font, gridLeft, searchY, COLS * SLOT, 18, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("検索..."));
        searchBox.setResponder(v -> rebuildList());
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);

        rebuildList();
    }

    /** 選択中のタブと検索語から、表示するアイテム一覧を作り直す */
    private void rebuildList() {
        shown.clear();
        scroll = 0;

        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        for (ItemStack stack : sourceItems()) {
            if (stack.isEmpty()) continue;
            if (!q.isEmpty() && !matches(stack, q)) continue;
            shown.add(stack);
        }
    }

    /** 選択中のタブの中身。「すべて」なら検索タブ(=全アイテム)を使う */
    private Iterable<ItemStack> sourceItems() {
        if (selectedTab != TAB_ALL && selectedTab < tabs.size()) {
            return tabs.get(selectedTab).getDisplayItems();
        }

        var all = CreativeModeTabs.searchTab().getDisplayItems();
        if (!all.isEmpty()) return all;

        // タブがまだ構築されていない場合の保険。登録済みアイテムから直接作る
        List<ItemStack> fallback = new ArrayList<>();
        for (var item : ForgeRegistries.ITEMS) fallback.add(new ItemStack(item));
        return fallback;
    }

    /** 表示名(日本語名を含む)とID の両方で引っかけられるようにする */
    private static boolean matches(ItemStack stack, String q) {
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.toString().toLowerCase(Locale.ROOT).contains(q);
    }

    private int maxScroll() {
        int rows = (shown.size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // タブの切り替え
        int tab = tabAt(mouseX, mouseY);
        if (tab != Integer.MIN_VALUE) {
            selectedTab = tab;
            rebuildList();
            return true;
        }

        // アイテムの選択(左右どちらのクリックでも指定できるようにしておく)
        if (button == 0 || button == 1) {
            ItemStack picked = itemAt(mouseX, mouseY);
            if (!picked.isEmpty()) {
                onPick.accept(picked.copy());
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** タブのセル番号。タブ上でなければ Integer.MIN_VALUE */
    private int tabAt(double mouseX, double mouseY) {
        int cellCount = tabs.size() + 1;
        for (int i = 0; i < cellCount; i++) {
            int cx = gridLeft + (i % COLS) * SLOT;
            int cy = tabTop + (i / COLS) * SLOT;
            if (mouseX >= cx && mouseX < cx + SLOT && mouseY >= cy && mouseY < cy + SLOT) {
                return i == 0 ? TAB_ALL : i - 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** グリッド上のアイテム。無ければ空 */
    private ItemStack itemAt(double mouseX, double mouseY) {
        int col = (int) ((mouseX - gridLeft) / SLOT);
        int row = (int) ((mouseY - gridTop) / SLOT);
        if (mouseX < gridLeft || mouseY < gridTop) return ItemStack.EMPTY;
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return ItemStack.EMPTY;

        int idx = (scroll + row) * COLS + col;
        return idx >= 0 && idx < shown.size() ? shown.get(idx) : ItemStack.EMPTY;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, "アイテムを選ぶ", this.width / 2, 12, 0xFFFFFF);

        // ---- タブ ----
        int cellCount = tabs.size() + 1;
        for (int i = 0; i < cellCount; i++) {
            int cx = gridLeft + (i % COLS) * SLOT;
            int cy = tabTop + (i / COLS) * SLOT;
            boolean active = (i == 0) ? selectedTab == TAB_ALL : selectedTab == i - 1;
            g.fill(cx, cy, cx + SLOT - 1, cy + SLOT - 1, active ? 0xFF6A6A6A : 0xFF2A2A2A);
            if (i == 0) {
                g.drawString(this.font, "全", cx + 5, cy + 5, 0xFFFFFF, false);
            } else {
                g.renderFakeItem(tabs.get(i - 1).getIconItem(), cx + 1, cy + 1);
            }
        }

        // ---- アイテムのグリッド ----
        g.fill(gridLeft - 1, gridTop - 1, gridLeft + COLS * SLOT, gridTop + ROWS * SLOT, 0xFF000000);

        ItemStack hovered = ItemStack.EMPTY;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (scroll + row) * COLS + col;
                int x = gridLeft + col * SLOT;
                int y = gridTop + row * SLOT;
                g.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0xFF3A3A3A);
                if (idx >= shown.size()) continue;

                ItemStack stack = shown.get(idx);
                g.renderFakeItem(stack, x + 1, y + 1);
                g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                    g.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0x60FFFFFF);
                    hovered = stack;
                }
            }
        }

        // スクロールバー
        int barX = gridLeft + COLS * SLOT + 3;
        int barH = ROWS * SLOT;
        g.fill(barX, gridTop, barX + 5, gridTop + barH, 0xFF202020);
        int max = maxScroll();
        int knobH = max == 0 ? barH : Math.max(12, barH / (max + ROWS) * ROWS);
        int knobY = max == 0 ? gridTop : gridTop + (barH - knobH) * scroll / max;
        g.fill(barX, knobY, barX + 5, knobY + knobH, 0xFF909090);

        super.render(g, mouseX, mouseY, partialTick);

        // ツールチップは最後に出す(グリッドや検索欄に隠されないように)
        int tab = tabAt(mouseX, mouseY);
        if (tab != Integer.MIN_VALUE) {
            Component name = tab == TAB_ALL
                    ? Component.literal("すべて")
                    : tabs.get(tab).getDisplayName();
            g.renderTooltip(this.font, name, mouseX, mouseY);
        } else if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mouseX, mouseY);
        }

        g.drawCenteredString(this.font, shown.size() + " 件  /  クリックで指定  ESCで戻る",
                this.width / 2, gridTop + ROWS * SLOT + 8, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
