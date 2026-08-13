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

import java.util.ArrayList;
import java.util.List;

/**
 * 会話設定アイテムを右クリックすると開く編集画面。
 *
 * 金床でのリネームは不要で、この画面の一番上でIDを直接入力する。
 * その下に「現在のページ」の各項目(名前/立ち絵/本文/効果音/文体)が並び、
 * ページの追加・削除・移動ができる。
 *
 * Tabキーで次の入力欄へ移動する(Shift+Tabで戻る)。
 *
 * 保存すると C2S_SaveDialoguePacket でサーバーへ送られ、
 * DialogueStore(ワールドデータ)に永続化される。
 */
public class DialogueEditorScreen extends Screen {

    private static final int FIELD_W = 300;
    private static final int FIELD_H = 18;
    private static final int LABEL_W = 78;

    private final List<DialoguePage> pages = new ArrayList<>();
    private int pageIndex = 0;

    private EditBox idBox;
    private EditBox nameBox;
    private EditBox portraitBox;
    private EditBox textBox;
    private EditBox soundBox;
    private Button styleButton;
    private Button portraitTypeButton;

    // Tab移動の順序。ここに並べた順で巡回する
    private final List<EditBox> tabOrder = new ArrayList<>();

    private PortraitSpec.Type portraitType = PortraitSpec.Type.NONE;
    private DialoguePage.TextStyle style = DialoguePage.TextStyle.NORMAL;

    private final String initialId;

    public DialogueEditorScreen(String initialId, DialogueSet existing) {
        super(Component.literal("会話設定"));
        this.initialId = initialId == null ? "" : initialId;
        if (existing != null && !existing.isEmpty()) {
            pages.addAll(existing.getPages());
        }
        if (pages.isEmpty()) {
            pages.add(DialoguePage.simple("", ""));
        }
    }

    @Override
    protected void init() {
        tabOrder.clear();
        int cx = this.width / 2;
        int x = cx - FIELD_W / 2 + LABEL_W;
        int w = FIELD_W - LABEL_W;
        int y = 34;

        // --- 一番上: 会話ID(これが金床の代わり) ---
        idBox = addField(x, y, w, "id");
        idBox.setValue(initialId);
        y += 30;

        // --- 以下は「現在のページ」の内容 ---
        nameBox = addField(x, y, w, "name"); y += 24;

        portraitBox = addField(x, y, w, "portrait");
        portraitTypeButton = Button.builder(Component.literal("NONE"), b -> cycderPortraitType())
                .bounds(cx - FIELD_W / 2 + LABEL_W + w + 4, y, 56, FIELD_H).build();
        addRenderableWidget(portraitTypeButton);
        y += 24;

        // 本文。EditBoxは1行なので、改行は \n と入力してもらう運用にする
        textBox = addField(x, y, w, "text");
        textBox.setMaxLength(512);
        y += 24;

        soundBox = addField(x, y, w, "sound"); y += 24;

        styleButton = Button.builder(Component.literal("NORMAL"), b -> cycleStyle())
                .bounds(x, y, 100, FIELD_H).build();
        addRenderableWidget(styleButton);
        y += 30;

        // --- ページ操作 ---
        int bw = 58;
        int bx = cx - FIELD_W / 2;
        addRenderableWidget(Button.builder(Component.literal("< 前"), b -> movePage(-1))
                .bounds(bx, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("次 >"), b -> movePage(1))
                .bounds(bx + bw + 4, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("ページ追加"), b -> addPage())
                .bounds(bx + (bw + 4) * 2, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("削除"), b -> removePage())
                .bounds(bx + (bw + 4) * 2 + 84, y, 58, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(Component.literal("保存して閉じる"), b -> saveAndClose())
                .bounds(cx - 100, y, 200, 20).build());

        loadPageIntoFields();
    }

    // 入力欄を1つ作って登録し、Tab移動の順序にも入れる
    private EditBox addField(int x, int y, int w, String hint) {
        EditBox box = new EditBox(this.font, x, y, w, FIELD_H, Component.literal(hint));
        box.setMaxLength(256);
        addRenderableWidget(box);
        tabOrder.add(box);
        return box;
    }

    private void cycderPortraitType() {
        PortraitSpec.Type[] vals = PortraitSpec.Type.values();
        portraitType = vals[(portraitType.ordinal() + 1) % vals.length];
        portraitTypeButton.setMessage(Component.literal(portraitType.name()));
    }

    private void cycleStyle() {
        DialoguePage.TextStyle[] vals = DialoguePage.TextStyle.values();
        style = vals[(style.ordinal() + 1) % vals.length];
        styleButton.setMessage(Component.literal(style.name()));
    }

    // 現在の入力内容を pages に書き戻す
    private void storeFieldsIntoPage() {
        if (pageIndex < 0 || pageIndex >= pages.size()) return;

        PortraitSpec spec = PortraitSpec.NONE;
        String pv = portraitBox.getValue().trim();
        if (portraitType != PortraitSpec.Type.NONE && !pv.isEmpty()) {
            try {
                spec = new PortraitSpec(portraitType, new ResourceLocation(pv));
            } catch (Exception ignored) {
                spec = PortraitSpec.NONE;
            }
        }
        // 本文の "\n" は実際の改行として保存する
        String text = textBox.getValue().replace("\\n", "\n");
        pages.set(pageIndex, new DialoguePage(
                nameBox.getValue(), text, spec, soundBox.getValue().trim(), style));
    }

    // pages の内容を入力欄へ反映する
    private void loadPageIntoFields() {
        DialoguePage p = pages.get(pageIndex);
        nameBox.setValue(p.speakerName());
        textBox.setValue(p.text().replace("\n", "\\n"));
        soundBox.setValue(p.soundId());
        portraitType = p.portrait().type();
        portraitBox.setValue(p.portrait().id() == null ? "" : p.portrait().id().toString());
        style = p.style();
        portraitTypeButton.setMessage(Component.literal(portraitType.name()));
        styleButton.setMessage(Component.literal(style.name()));
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
        if (pages.size() <= 1) return; // 最低1ページは残す
        pages.remove(pageIndex);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        loadPageIntoFields();
    }

    private void saveAndClose() {
        storeFieldsIntoPage();
        String id = idBox.getValue().trim().replace(' ', '_');
        if (id.isEmpty()) return; // IDが無ければ保存しない

        DialogueSet set = new DialogueSet(id);
        for (DialoguePage p : pages) set.addPage(p);
        ModMessages.sendToServer(new C2S_SaveDialoguePacket(set));
        onClose();
    }

    /** Tabキーで次の入力欄へ移動する(Shift+Tabで戻る) */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) { // Tab
            boolean backwards = (modifiers & 0x0001) != 0; // Shift
            int current = -1;
            for (int i = 0; i < tabOrder.size(); i++) {
                if (tabOrder.get(i).isFocused()) { current = i; break; }
            }
            int next = current < 0 ? 0
                    : (current + (backwards ? -1 : 1) + tabOrder.size()) % tabOrder.size();
            for (EditBox b : tabOrder) b.setFocused(false);
            tabOrder.get(next).setFocused(true);
            this.setFocused(tabOrder.get(next));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int left = cx - FIELD_W / 2;

        g.drawCenteredString(this.font, "会話設定  (ページ " + (pageIndex + 1) + " / " + pages.size() + ")",
                cx, 14, 0xFFFFFF);

        // 各入力欄のラベル
        drawLabel(g, left, 34, "id");
        drawLabel(g, left, 64, "name");
        drawLabel(g, left, 88, "portrait");
        drawLabel(g, left, 112, "text");
        drawLabel(g, left, 136, "sound");
        drawLabel(g, left, 160, "style");

        super.render(g, mouseX, mouseY, partialTick);

        g.drawCenteredString(this.font, "Tabで次の項目へ / 本文の改行は \\n",
                cx, this.height - 14, 0xA0A0A0);
    }

    private void drawLabel(GuiGraphics g, int x, int y, String text) {
        g.drawString(this.font, text, x, y + 5, 0xC0C0C0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
