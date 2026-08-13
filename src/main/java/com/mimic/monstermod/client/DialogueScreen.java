package com.mimic.monstermod.client;

import com.mimic.monstermod.dialogue.DialoguePage;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.PortraitSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
 * 仕様:
 *  - タイプライター表示はせず、最初から全文表示。クリック/Enter/Spaceで次ページ
 *  - ESC(または最終ページで進む)で即座に閉じる
 *  - ゲームを止めない(isPauseScreen=false)。会話中も攻撃される
 *  - 左に立ち絵(画像 or エンティティモデル)。無い場合はテキストを左まで広げる
 *  - 名前は自由入力("???"などもそのまま表示)
 *
 * 再生開始は S2C_StartDialoguePacket から。
 */
public class DialogueScreen extends Screen {

    // ---- レイアウト定数(見た目の調整はここ) ----
    private static final int BOX_HEIGHT   = 100; // ウィンドウの高さ
    private static final int BOX_MARGIN_X = 20;  // 画面左右の余白
    private static final int BOX_MARGIN_B = 16;  // 画面下からの余白
    private static final int PADDING      = 12;  // ウィンドウ内側の余白
    private static final int PORTRAIT     = 76;  // 立ち絵の一辺
    private static final int LINE_GAP     = 4;   // 行間の追加分

    // 背景の黒。0xC0=約75%。50%だと草原などで文字が読みにくいため濃いめにしてある
    private static final int COLOR_BG      = 0xC0000000;
    private static final int COLOR_BORDER  = 0xFFFFFFFF; // 外枠(白)
    private static final int COLOR_BORDER2 = 0xFF000000; // 内側の締め(黒)
    private static final int COLOR_NAME    = 0xFFFFE080;
    private static final int COLOR_TEXT    = 0xFFFFFFFF;

    private final DialogueSet set;
    private int pageIndex = 0;

    // ENTITY立ち絵用。ページごとに生成してキャッシュする(毎フレーム作らない)
    private LivingEntity portraitEntity;
    private ResourceLocation portraitEntityId;

    private int tickCounter;

    public DialogueScreen(DialogueSet set) {
        super(Component.literal("Dialogue"));
        this.set = set;
    }

    /** ゲームを止めない。会話中も攻撃されるという要件のため必須 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        playPageSound();
    }

    @Override
    public void tick() {
        tickCounter++;
    }

    private DialoguePage current() {
        return set.getPages().get(Math.min(pageIndex, set.getPages().size() - 1));
    }

    // ページを進める。最終ページなら閉じる
    private void advance() {
        pageIndex++;
        if (pageIndex >= set.getPages().size()) {
            onClose();
            return;
        }
        portraitEntity = null; // 立ち絵が変わる可能性があるので作り直す
        portraitEntityId = null;
        playPageSound();
    }

    // ページに設定された効果音を鳴らす(未指定なら何もしない)
    private void playPageSound() {
        DialoguePage page = current();
        ResourceLocation id = page.soundLocation();
        if (id == null) return;
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (ev == null) return;
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(ev, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            advance();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC は Screen 既定の onClose に任せる(即座にゲームへ戻る)
        if (keyCode == 257 || keyCode == 335 || keyCode == 32) { // Enter / テンキーEnter / Space
            advance();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 背景の暗転はしない(会話中もゲームが見えていてほしいため renderBackground を呼ばない)
        DialoguePage page = current();

        int boxLeft   = BOX_MARGIN_X;
        int boxRight  = this.width - BOX_MARGIN_X;
        int boxBottom = this.height - BOX_MARGIN_B;
        int boxTop    = boxBottom - BOX_HEIGHT;

        drawFrame(g, boxLeft, boxTop, boxRight, boxBottom);

        // 立ち絵の有無でテキスト開始位置を変える(無ければ左まで詰める)
        boolean hasPortrait = !page.portrait().isNone();
        int textLeft = boxLeft + PADDING;
        if (hasPortrait) {
            int px = boxLeft + PADDING;
            int py = boxTop + (BOX_HEIGHT - PORTRAIT) / 2;
            drawPortrait(g, page.portrait(), px, py, mouseX, mouseY);
            textLeft = px + PORTRAIT + PADDING;
        }

        // 名前(右側の上)
        int y = boxTop + PADDING;
        if (page.speakerName() != null && !page.speakerName().isEmpty()) {
            g.drawString(this.font, page.speakerName(), textLeft, y, COLOR_NAME, true);
            y += this.font.lineHeight + 6;
        }

        // 本文(名前の下)。ウィンドウ幅で自動折り返しする
        int textWidth = (boxRight - PADDING) - textLeft;
        drawBody(g, page, textLeft, y, textWidth);

        // 続きがあることを示すマーカー(右下で点滅)
        if (pageIndex < set.getPages().size() - 1 && (tickCounter / 8) % 2 == 0) {
            g.drawString(this.font, "▼", boxRight - PADDING - 8, boxBottom - PADDING - 4, COLOR_TEXT, true);
        }
    }

    // 黒背景 + 白枠 + 内側の黒線という二重枠を描く(明暗を分けて輪郭を出す)
    private void drawFrame(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, COLOR_BG);          // 半透明の黒背景
        drawRect(g, left, top, right, bottom, COLOR_BORDER); // 外枠(白)
        drawRect(g, left + 1, top + 1, right - 1, bottom - 1, COLOR_BORDER2); // 内側(黒)
    }

    private void drawRect(GuiGraphics g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    // 本文を折り返して描画する。文体(SHAKE/WAVE)は行ごとに座標を揺らして表現する
    private void drawBody(GuiGraphics g, DialoguePage page, int x, int y, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String raw : page.text().split("\n", -1)) {
            lines.addAll(this.font.split(Component.literal(raw), Math.max(16, width)));
        }

        int lineH = this.font.lineHeight + LINE_GAP;
        for (int i = 0; i < lines.size(); i++) {
            int dx = 0, dy = 0;
            switch (page.style()) {
                case SHAKE -> {
                    // 脅し文句などの震え。毎フレーム細かく揺らす
                    dx = (int) (Math.round(Math.sin((tickCounter + i * 3) * 2.7) * 1.2));
                    dy = (int) (Math.round(Math.cos((tickCounter + i * 5) * 3.1) * 1.2));
                }
                case WAVE -> dy = (int) Math.round(Math.sin((tickCounter * 0.25) + i * 0.6) * 1.5);
                default -> { }
            }
            g.drawString(this.font, lines.get(i), x + dx, y + i * lineH + dy, COLOR_TEXT, true);
        }
    }

    // 立ち絵を描く。IMAGEはテクスチャをそのまま、ENTITYは実際のモデルを描画する
    private void drawPortrait(GuiGraphics g, PortraitSpec spec, int x, int y, int mouseX, int mouseY) {
        switch (spec.type()) {
            case IMAGE -> g.blit(spec.id(), x, y, 0, 0, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT);
            case ENTITY -> {
                LivingEntity e = getOrCreatePortraitEntity(spec.id());
                if (e != null) {
                    // GeckoLib製のモデルもバニラのEntityRenderDispatcherを通るのでそのまま描画できる
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

    // ENTITY立ち絵用の表示専用エンティティ。毎フレーム生成しないようキャッシュする
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
