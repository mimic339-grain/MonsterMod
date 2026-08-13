package com.mimic.monstermod.client;

import com.mimic.monstermod.dialogue.DialoguePage;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.PortraitSpec;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SaveDialoguePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 会話設定アイテムを右クリックすると開く編集画面。
 *
 * 一番上で会話IDを直接入力する(金床でのリネームは不要)。
 * その下は「現在のページ」の内容で、ページの追加・削除・移動ができる。
 *
 * Tabキーの挙動(コマンド入力と同じ感覚):
 *   候補が出ている入力欄  → Tabで候補を確定(補完)。連打で次の候補へ
 *   候補が無い入力欄      → Tabで次の入力欄へ移動(Shift+Tabで戻る)
 * portrait と sound は登録済みのIDから候補を出すため、全部手入力する必要がない。
 */
public class DialogueEditorScreen extends Screen {

    private static final int FIELD_W = 320;
    private static final int FIELD_H = 18;
    private static final int LABEL_W = 78;
    private static final int MAX_SUGGEST = 6;

    private final List<DialoguePage> pages = new ArrayList<>();
    private int pageIndex = 0;

    private EditBox idBox, nameBox, portraitBox, textBox, soundBox, cpsBox;
    private Button styleButton, portraitTypeButton;

    private final List<EditBox> tabOrder = new ArrayList<>();

    private PortraitSpec.Type portraitType = PortraitSpec.Type.NONE;
    private DialoguePage.TextStyle style = DialoguePage.TextStyle.NORMAL;
    private int typewriterCps = 0;

    private final String initialId;

    // 補完候補の状態
    private final List<String> suggestions = new ArrayList<>();
    private int suggestIndex = 0;
    private EditBox suggestOwner;

    public DialogueEditorScreen(String initialId, DialogueSet existing) {
        super(Component.literal("会話設定"));
        this.initialId = initialId == null ? "" : initialId;
        if (existing != null && !existing.isEmpty()) pages.addAll(existing.getPages());
        if (pages.isEmpty()) pages.add(DialoguePage.simple("", ""));
    }

    @Override
    protected void init() {
        tabOrder.clear();
        int cx = this.width / 2;
        int x = cx - FIELD_W / 2 + LABEL_W;
        int w = FIELD_W - LABEL_W;
        int y = 30;

        idBox = addField(x, y, w); idBox.setValue(initialId); y += 28;

        nameBox = addField(x, y, w); y += 22;

        portraitBox = addField(x, y, w - 60);
        portraitTypeButton = Button.builder(Component.literal("NONE"), b -> cyclePortraitType())
                .bounds(x + w - 56, y, 56, FIELD_H).build();
        addRenderableWidget(portraitTypeButton);
        y += 22;

        textBox = addField(x, y, w); textBox.setMaxLength(1024); y += 22;

        soundBox = addField(x, y, w); y += 22;

        styleButton = Button.builder(Component.literal("NORMAL"), b -> cycleStyle())
                .bounds(x, y, 90, FIELD_H).build();
        addRenderableWidget(styleButton);

        // タイプライター速度(0で全文即表示)
        cpsBox = new EditBox(this.font, x + 96, y, 60, FIELD_H, Component.literal("cps"));
        cpsBox.setMaxLength(4);
        cpsBox.setFilter(sv -> sv.isEmpty() || sv.chars().allMatch(Character::isDigit));
        addRenderableWidget(cpsBox);
        tabOrder.add(cpsBox);
        y += 30;

        int bw = 56, bx = cx - FIELD_W / 2;
        addRenderableWidget(Button.builder(Component.literal("< 前"), b -> movePage(-1)).bounds(bx, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("次 >"), b -> movePage(1)).bounds(bx + bw + 4, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("ページ追加"), b -> addPage()).bounds(bx + (bw + 4) * 2, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("削除"), b -> removePage()).bounds(bx + (bw + 4) * 2 + 84, y, 56, 20).build());
        y += 26;

        addRenderableWidget(Button.builder(Component.literal("保存して閉じる"), b -> saveAndClose())
                .bounds(cx - 100, y, 200, 20).build());

        loadPageIntoFields();
    }

    private EditBox addField(int x, int y, int w) {
        EditBox box = new EditBox(this.font, x, y, w, FIELD_H, Component.empty());
        box.setMaxLength(256);
        box.setResponder(v -> updateSuggestions(box)); // 入力のたびに候補を作り直す
        addRenderableWidget(box);
        tabOrder.add(box);
        return box;
    }

    // ---- 補完候補 ----

    /** 入力中の欄に応じて候補を作る。portrait と sound だけ対象 */
    private void updateSuggestions(EditBox box) {
        suggestions.clear();
        suggestIndex = 0;
        suggestOwner = box;

        String v = box.getValue().toLowerCase(Locale.ROOT);
        if (box == soundBox) {
            for (ResourceLocation id : ForgeRegistries.SOUND_EVENTS.getKeys()) {
                String s = id.toString();
                if (s.toLowerCase(Locale.ROOT).contains(v)) suggestions.add(s);
                if (suggestions.size() >= 200) break;
            }
        } else if (box == portraitBox && portraitType == PortraitSpec.Type.ENTITY) {
            for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                String s = id.toString();
                if (s.toLowerCase(Locale.ROOT).contains(v)) suggestions.add(s);
                if (suggestions.size() >= 200) break;
            }
        }
        // 自分のMODのものを先に出す(探しやすくするため)
        suggestions.sort((a, b) -> {
            boolean am = a.startsWith("monstermod:"), bm = b.startsWith("monstermod:");
            if (am != bm) return am ? -1 : 1;
            return a.compareTo(b);
        });
    }

    private boolean hasSuggestions() {
        return suggestOwner != null && suggestOwner.isFocused() && !suggestions.isEmpty();
    }

    // ---- 各種操作 ----

    private void cyclePortraitType() {
        PortraitSpec.Type[] vals = PortraitSpec.Type.values();
        portraitType = vals[(portraitType.ordinal() + 1) % vals.length];
        portraitTypeButton.setMessage(Component.literal(portraitType.name()));
        updateSuggestions(portraitBox);
    }

    private void cycleStyle() {
        DialoguePage.TextStyle[] vals = DialoguePage.TextStyle.values();
        style = vals[(style.ordinal() + 1) % vals.length];
        styleButton.setMessage(Component.literal(style.name()));
    }

    private void storeFieldsIntoPage() {
        if (pageIndex < 0 || pageIndex >= pages.size()) return;

        PortraitSpec spec = PortraitSpec.NONE;
        String pv = portraitBox.getValue().trim();
        if (portraitType != PortraitSpec.Type.NONE && !pv.isEmpty()) {
            try { spec = new PortraitSpec(portraitType, new ResourceLocation(pv)); }
            catch (Exception ignored) { spec = PortraitSpec.NONE; }
        }
        try { typewriterCps = cpsBox.getValue().isEmpty() ? 0 : Integer.parseInt(cpsBox.getValue()); }
        catch (NumberFormatException e) { typewriterCps = 0; }

        String text = textBox.getValue().replace("\\n", "\n");
        pages.set(pageIndex, new DialoguePage(
                nameBox.getValue(), text, spec, soundBox.getValue().trim(), style, typewriterCps));
    }

    private void loadPageIntoFields() {
        DialoguePage p = pages.get(pageIndex);
        nameBox.setValue(p.speakerName());
        textBox.setValue(p.text().replace("\n", "\\n"));
        soundBox.setValue(p.soundId());
        portraitType = p.portrait().type();
        portraitBox.setValue(p.portrait().id() == null ? "" : p.portrait().id().toString());
        style = p.style();
        typewriterCps = p.typewriterCps();
        cpsBox.setValue(String.valueOf(typewriterCps));
        portraitTypeButton.setMessage(Component.literal(portraitType.name()));
        styleButton.setMessage(Component.literal(style.name()));
        suggestions.clear();
    }

    private void movePage(int delta) {
        storeFieldsIntoPage();
        int next = pageIndex + delta;
        if (next < 0 || next >= pages.size()) return;
        pageIndex = next;
        loadPageIntoFields();
    }

    private void addPage() {
        storeFieldsIntoPage();
        pages.add(pageIndex + 1, DialoguePage.simple("", ""));
        pageIndex++;
        loadPageIntoFields();
    }

    private void removePage() {
        if (pages.size() <= 1) return;
        pages.remove(pageIndex);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        loadPageIntoFields();
    }

    private void saveAndClose() {
        storeFieldsIntoPage();
        String id = idBox.getValue().trim().replace(' ', '_');
        if (id.isEmpty()) return;
        DialogueSet set = new DialogueSet(id);
        for (DialoguePage p : pages) set.addPage(p);
        ModMessages.sendToServer(new C2S_SaveDialoguePacket(set));
        onClose();
    }

    /**
     * Tab: 候補があれば補完、無ければ次の入力欄へ移動。
     * 上下キーで候補を選べる。
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) { // Tab
            if (hasSuggestions()) {
                suggestOwner.setValue(suggestions.get(suggestIndex % suggestions.size()));
                suggestIndex++;
                return true;
            }
            focusNextField((modifiers & 0x0001) != 0); // Shiftで逆順
            return true;
        }
        if (hasSuggestions() && (keyCode == 264 || keyCode == 265)) { // ↓ / ↑
            suggestIndex = Math.max(0, suggestIndex + (keyCode == 264 ? 1 : -1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 入力欄のフォーカスを移す。Screenのフォーカス管理と食い違わないよう setFocused を使う */
    private void focusNextField(boolean backwards) {
        int current = -1;
        for (int i = 0; i < tabOrder.size(); i++) {
            if (tabOrder.get(i).isFocused()) { current = i; break; }
        }
        int next = current < 0 ? 0 : (current + (backwards ? -1 : 1) + tabOrder.size()) % tabOrder.size();
        for (EditBox b : tabOrder) b.setFocused(false);
        EditBox target = tabOrder.get(next);
        target.setFocused(true);
        this.setFocused(target);
        suggestions.clear();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int left = cx - FIELD_W / 2;

        g.drawCenteredString(this.font, "会話設定  (ページ " + (pageIndex + 1) + " / " + pages.size() + ")",
                cx, 12, 0xFFFFFF);

        drawLabel(g, left, 30,  "id");
        drawLabel(g, left, 58,  "name");
        drawLabel(g, left, 80,  "portrait");
        drawLabel(g, left, 102, "text");
        drawLabel(g, left, 124, "sound");
        drawLabel(g, left, 146, "style / 速度");

        super.render(g, mouseX, mouseY, partialTick);

        drawSuggestions(g);

        g.drawCenteredString(this.font,
                "Tab: 候補を補完 / 候補が無ければ次の欄へ   改行=\\n   色=&c など   速度0で全文表示",
                cx, this.height - 12, 0xA0A0A0);
    }

    /** 入力欄の下に候補を並べて表示する */
    private void drawSuggestions(GuiGraphics g) {
        if (!hasSuggestions()) return;
        int x = suggestOwner.getX();
        int y = suggestOwner.getY() + FIELD_H + 1;
        int w = Math.max(160, suggestOwner.getWidth());
        int n = Math.min(MAX_SUGGEST, suggestions.size());

        g.fill(x, y, x + w, y + n * 11 + 2, 0xE0000000);
        for (int i = 0; i < n; i++) {
            int idx = (suggestIndex + i) % suggestions.size();
            boolean sel = (i == 0);
            g.drawString(this.font, suggestions.get(idx), x + 3, y + 2 + i * 11,
                    sel ? 0xFFFFA0 : 0xB0B0B0, false);
        }
        if (suggestions.size() > n) {
            g.drawString(this.font, "…他 " + (suggestions.size() - n) + " 件",
                    x + 3, y + 2 + n * 11, 0x707070, false);
        }
    }

    private void drawLabel(GuiGraphics g, int x, int y, String text) {
        g.drawString(this.font, text, x, y + 5, 0xC0C0C0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
