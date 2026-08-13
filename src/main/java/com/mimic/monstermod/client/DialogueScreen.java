package com.mimic.monstermod.client;

import com.mimic.monstermod.dialogue.DialoguePage;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.DialogueText;
import com.mimic.monstermod.dialogue.PortraitSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * RPG風の会話ウィンドウ。
 *
 * 表示ルール:
 *  - 本文は読みやすいよう拡大表示し、1画面あたり最大3行に制限する
 *  - 3行を超える本文は自動で次のページへ分割される。\n を入れれば任意の位置で改行できる
 *  - 色は「&」記法(例: &c赤&r通常)。名前・本文どちらでも、部分的にも使える
 *  - タイプライター表示はページ単位で任意(typewriterCps>0のとき)。
 *    表示途中でクリックすると全文即表示、表示済みならクリックで次ページ
 *  - ESCで即座に閉じる。isPauseScreen=false なのでゲームは止まらず攻撃もされる
 */
public class DialogueScreen extends Screen {

    // ---- レイアウト(見た目の調整はここ) ----
    private static final int   BOX_HEIGHT   = 104;
    private static final int   BOX_MARGIN_X = 20;
    private static final int   BOX_MARGIN_B = 16;
    private static final int   PADDING      = 12;
    private static final int   PORTRAIT     = 80;
    private static final float TEXT_SCALE   = 1.5f; // 本文の拡大率
    private static final float NAME_SCALE   = 1.3f;
    private static final int   MAX_LINES    = 3;    // 1ページに表示する最大行数
    private static final int   LINE_GAP     = 5;

    private static final int COLOR_BG      = 0xC0000000; // 約75%の黒
    private static final int COLOR_BORDER  = 0xFFFFFFFF;
    private static final int COLOR_BORDER2 = 0xFF000000;
    private static final int COLOR_NAME    = 0xFFFFE080;
    private static final int COLOR_TEXT    = 0xFFFFFFFF;

    /** 実際に1画面ぶんとして表示する単位(元の1ページが複数に分割されることがある) */
    private record DisplayPage(DialoguePage source, List<String> lines, String rawJoined) {}

    private final DialogueSet set;
    private final List<DisplayPage> displayPages = new ArrayList<>();
    private int pageIndex = 0;

    private LivingEntity portraitEntity;
    private ResourceLocation portraitEntityId;

    private int tickCounter;
    private int pageTicks;      // 現在ページを表示し始めてからの経過tick(タイプライター用)
    private boolean revealAll;  // クリックで全文表示させたか

    public DialogueScreen(DialogueSet set) {
        super(Component.literal("Dialogue"));
        this.set = set;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 会話中もゲームを止めない(攻撃される)
    }

    @Override
    protected void init() {
        buildDisplayPages();
        pageIndex = 0;
        pageTicks = 0;
        revealAll = false;
        playPageSound();
    }

    /**
     * 元のページを「最大3行」の表示ページへ分割する。
     * 折り返し幅は立ち絵の有無と画面幅で決まるため、画面サイズが確定したここで組み立てる。
     */
    private void buildDisplayPages() {
        displayPages.clear();

        int boxLeft  = BOX_MARGIN_X;
        int boxRight = this.width - BOX_MARGIN_X;

        for (DialoguePage page : set.getPages()) {
            boolean hasPortrait = !page.portrait().isNone();
            int textLeft = boxLeft + PADDING + (hasPortrait ? PORTRAIT + PADDING : 0);
            int available = (boxRight - PADDING) - textLeft;
            // 拡大して描くので、折り返し判定は拡大前の幅で行う
            int wrapWidth = Math.max(32, (int) (available / TEXT_SCALE));

            // \n で明示的に改行し、さらに幅で自動折り返しする
            List<String> allLines = new ArrayList<>();
            for (String paragraph : DialogueText.colorize(page.text()).split("\n", -1)) {
                if (paragraph.isEmpty()) { allLines.add(""); continue; }
                allLines.addAll(wrapPreservingCodes(paragraph, wrapWidth));
            }

            // 3行ずつに区切って別ページにする
            for (int i = 0; i < allLines.size(); i += MAX_LINES) {
                List<String> chunk = new ArrayList<>(
                        allLines.subList(i, Math.min(i + MAX_LINES, allLines.size())));
                displayPages.add(new DisplayPage(page, chunk, String.join("\n", chunk)));
            }
            if (allLines.isEmpty()) {
                displayPages.add(new DisplayPage(page, List.of(""), ""));
            }
        }
        if (displayPages.isEmpty()) {
            displayPages.add(new DisplayPage(DialoguePage.simple("", ""), List.of(""), ""));
        }
    }

    /**
     * 装飾コード(§)を保持したまま指定幅で折り返す。
     *
     * font.split は § を Style に変換するため、行を文字列へ戻すと色が失われてしまう。
     * また日本語には単語区切りが無いので文字単位で折り返す必要がある。
     * 行をまたいでも色が続くよう、有効な装飾を次の行の先頭へ引き継ぐ。
     */
    private List<String> wrapPreservingCodes(String colorized, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String carry = "";   // 次の行の先頭へ引き継ぐ装飾
        String active = "";  // 現在有効な装飾

        for (int i = 0; i < colorized.length(); i++) {
            char c = colorized.charAt(i);
            if (c == '§' && i + 1 < colorized.length()) {
                String code = colorized.substring(i, i + 2);
                char k = code.charAt(1);
                active = (k == 'r' || k == 'R') ? "" : active + code;
                line.append(code);
                i++;
                continue;
            }
            if (this.font.width(line.toString() + c) > width && line.length() > 0) {
                out.add(carry + line);
                carry = active;
                line.setLength(0);
            }
            line.append(c);
        }
        if (line.length() > 0 || out.isEmpty()) out.add(carry + line);
        return out;
    }

    private DisplayPage current() {
        return displayPages.get(Math.min(pageIndex, displayPages.size() - 1));
    }

    @Override
    public void tick() {
        tickCounter++;
        pageTicks++;
    }

    /** そのページを全部表示し終えているか(タイプライター用) */
    private boolean isFullyRevealed() {
        DisplayPage dp = current();
        int cps = dp.source().typewriterCps();
        if (cps <= 0 || revealAll) return true;
        int shown = (int) ((pageTicks / 20.0) * cps);
        return shown >= DialogueText.visibleLength(dp.rawJoined());
    }

    private void advance() {
        // タイプライター表示の途中なら、まず全文表示にする
        if (!isFullyRevealed()) {
            revealAll = true;
            return;
        }
        pageIndex++;
        if (pageIndex >= displayPages.size()) {
            onClose();
            return;
        }
        pageTicks = 0;
        revealAll = false;
        portraitEntity = null;
        portraitEntityId = null;
        playPageSound();
    }

    private void playPageSound() {
        ResourceLocation id = current().source().soundLocation();
        if (id == null) return;
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (ev == null) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { advance(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335 || keyCode == 32) { // Enter / テンキーEnter / Space
            advance();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // ESCは既定の onClose
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        DisplayPage dp = current();
        DialoguePage page = dp.source();

        int boxLeft   = BOX_MARGIN_X;
        int boxRight  = this.width - BOX_MARGIN_X;
        int boxBottom = this.height - BOX_MARGIN_B;
        int boxTop    = boxBottom - BOX_HEIGHT;

        drawFrame(g, boxLeft, boxTop, boxRight, boxBottom);

        boolean hasPortrait = !page.portrait().isNone();
        int textLeft = boxLeft + PADDING;
        if (hasPortrait) {
            int px = boxLeft + PADDING;
            int py = boxTop + (BOX_HEIGHT - PORTRAIT) / 2;
            drawPortrait(g, page.portrait(), px, py, mouseX, mouseY);
            textLeft = px + PORTRAIT + PADDING;
        }

        int y = boxTop + PADDING;

        // 名前(色記法つき)
        String name = DialogueText.colorize(page.speakerName());
        if (!name.isEmpty()) {
            drawScaled(g, name, textLeft, y, NAME_SCALE, COLOR_NAME);
            y += (int) (this.font.lineHeight * NAME_SCALE) + 6;
        }

        // 本文(最大3行)。タイプライター中は途中まで
        int cps = page.typewriterCps();
        int allow = Integer.MAX_VALUE;
        if (cps > 0 && !revealAll) {
            allow = (int) ((pageTicks / 20.0) * cps);
        }

        int lineH = (int) (this.font.lineHeight * TEXT_SCALE) + LINE_GAP;
        int used = 0;
        for (int i = 0; i < dp.lines().size(); i++) {
            String line = dp.lines().get(i);
            String shown = line;
            if (allow != Integer.MAX_VALUE) {
                int remain = allow - used;
                if (remain <= 0) break;
                shown = DialogueText.takeVisible(line, remain);
                used += DialogueText.visibleLength(line);
            }

            int dx = 0, dy = 0;
            switch (page.style()) {
                case SHAKE -> {
                    dx = (int) Math.round(Math.sin((tickCounter + i * 3) * 2.7) * 1.5);
                    dy = (int) Math.round(Math.cos((tickCounter + i * 5) * 3.1) * 1.5);
                }
                case WAVE -> dy = (int) Math.round(Math.sin((tickCounter * 0.25) + i * 0.6) * 2.0);
                default -> { }
            }
            drawScaled(g, shown, textLeft + dx, y + i * lineH + dy, TEXT_SCALE, COLOR_TEXT);
        }

        // 続きがある場合の点滅マーカー(全文表示済みのときだけ出す)
        if (isFullyRevealed() && pageIndex < displayPages.size() - 1 && (tickCounter / 8) % 2 == 0) {
            g.drawString(this.font, "▼", boxRight - PADDING - 8, boxBottom - PADDING - 4, COLOR_TEXT, true);
        }
    }

    /** 文字を拡大して描く。GuiGraphicsのdrawStringは等倍なのでPoseStackで拡大する */
    private void drawScaled(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, text, 0, 0, color, true);
        g.pose().popPose();
    }

    private void drawFrame(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, COLOR_BG);
        drawRect(g, left, top, right, bottom, COLOR_BORDER);
        drawRect(g, left + 1, top + 1, right - 1, bottom - 1, COLOR_BORDER2);
    }

    private void drawRect(GuiGraphics g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    private void drawPortrait(GuiGraphics g, PortraitSpec spec, int x, int y, int mouseX, int mouseY) {
        switch (spec.type()) {
            case IMAGE -> g.blit(spec.id(), x, y, 0, 0, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT);
            case ENTITY -> {
                LivingEntity e = getOrCreatePortraitEntity(spec.id());
                if (e != null) {
                    int scale = (int) (PORTRAIT * 0.45f / Math.max(0.5f, e.getBbHeight()) * 2.0f);
                    scale = Math.max(8, Math.min(80, scale));
                    InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, x + PORTRAIT / 2, y + PORTRAIT - 6, scale,
                            (x + PORTRAIT / 2f) - mouseX, (y + PORTRAIT / 2f) - mouseY, e);
                }
            }
            default -> { }
        }
    }

    private LivingEntity getOrCreatePortraitEntity(ResourceLocation id) {
        if (portraitEntity != null && id.equals(portraitEntityId)) return portraitEntity;
        if (this.minecraft == null || this.minecraft.level == null) return null;

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;
        Entity created = type.create(this.minecraft.level);
        if (!(created instanceof LivingEntity living)) return null;

        portraitEntity = living;
        portraitEntityId = id;
        return portraitEntity;
    }
}
