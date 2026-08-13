package com.mimic.monstermod.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SaveNpcSettingsPacket;
import com.mimic.monstermod.npc.NpcSettings;
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
 * NPC作成ツールの設定画面。
 *
 * 「どのMobを置くか」と「NPCとしてどう固定するか」をここで決める。
 * Mob名は登録済みのIDから候補を出すので、Tabで補完できる(他MODのMobも一覧に出る)。
 */
public class NpcEditorScreen extends Screen {

    private static final int FIELD_W = 300;
    private static final int FIELD_H = 18;
    private static final int MAX_SUGGEST = 6;

    private EditBox typeBox;
    private Button lookBtn, moveBtn;

    private final String initialType;
    private NpcSettings.Movement movement;
    private boolean attacksPlayers, allowRetaliation, invulnerable;
    private NpcSettings.LookMode lookMode;

    private final List<String> suggestions = new ArrayList<>();
    private int suggestIndex = 0;

    public NpcEditorScreen(String typeId, NpcSettings s) {
        super(Component.literal("NPC設定"));
        this.initialType = typeId == null ? "minecraft:villager" : typeId;
        this.movement = s.movement();
        this.attacksPlayers = s.attacksPlayers();
        this.allowRetaliation = s.allowRetaliation();
        this.invulnerable = s.invulnerable();
        this.lookMode = s.lookMode();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int x = cx - FIELD_W / 2;
        int y = 44;

        typeBox = new EditBox(this.font, x + 80, y, FIELD_W - 80, FIELD_H, Component.empty());
        typeBox.setMaxLength(128);
        typeBox.setValue(initialType);
        typeBox.setResponder(v -> updateSuggestions());
        addRenderableWidget(typeBox);
        y += 34;

        // 移動: 固定 か 自由に動き回るか
        moveBtn = Button.builder(moveLabel(), b -> {
            movement = movement == NpcSettings.Movement.FIXED
                    ? NpcSettings.Movement.FREE : NpcSettings.Movement.FIXED;
            moveBtn.setMessage(moveLabel());
        }).bounds(x, y, FIELD_W, 20).build();
        addRenderableWidget(moveBtn); y += 24;

        addToggle(x, y, "プレイヤーを襲う", () -> attacksPlayers, v -> attacksPlayers = v); y += 22;
        addToggle(x, y, "反撃する", () -> allowRetaliation, v -> allowRetaliation = v); y += 22;
        addToggle(x, y, "ダメージを受けない", () -> invulnerable, v -> invulnerable = v); y += 26;

        lookBtn = Button.builder(lookLabel(), b -> {
            NpcSettings.LookMode[] vals = NpcSettings.LookMode.values();
            lookMode = vals[(lookMode.ordinal() + 1) % vals.length];
            lookBtn.setMessage(lookLabel());
        }).bounds(x, y, FIELD_W, 20).build();
        addRenderableWidget(lookBtn);
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("保存して閉じる"), b -> saveAndClose())
                .bounds(cx - 100, y, 200, 20).build());
    }

    private Component lookLabel() {
        return Component.literal(switch (lookMode) {
            case FREE -> "視点: 元のまま";
            case LOOK_PLAYER -> "視点: プレイヤーを見続ける";
            case FIXED -> "視点: 固定";
        });
    }

    private Component moveLabel() {
        return Component.literal(movement == NpcSettings.Movement.FIXED
                ? "移動: 固定(水で流されず落ちない・押されない)"
                : "移動: 自由に動き回る(元のAIのまま)");
    }

    private Button addToggle(int x, int y, String label,
                             java.util.function.BooleanSupplier getter,
                             java.util.function.Consumer<Boolean> setter) {
        Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.literal(label + ": " + (getter.getAsBoolean() ? "ON" : "OFF")), b -> {
            boolean nv = !getter.getAsBoolean();
            setter.accept(nv);
            b.setMessage(Component.literal(label + ": " + (nv ? "ON" : "OFF")));
        }).bounds(x, y, FIELD_W, 20).build();
        addRenderableWidget(holder[0]);
        return holder[0];
    }

    /** 登録済みのMob一覧から候補を作る。他MODのMobもここに出る */
    private void updateSuggestions() {
        suggestions.clear();
        suggestIndex = 0;
        String v = typeBox.getValue().toLowerCase(Locale.ROOT);
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            String s = id.toString();
            if (s.toLowerCase(Locale.ROOT).contains(v)) suggestions.add(s);
            if (suggestions.size() >= 200) break;
        }
        suggestions.sort((a, b) -> {
            boolean am = a.startsWith("monstermod:"), bm = b.startsWith("monstermod:");
            if (am != bm) return am ? -1 : 1;
            return a.compareTo(b);
        });
    }

    private boolean hasSuggestions() {
        return typeBox != null && typeBox.isFocused() && !suggestions.isEmpty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258 && hasSuggestions()) { // Tabで補完
            typeBox.setValue(suggestions.get(suggestIndex % suggestions.size()));
            suggestIndex++;
            return true;
        }
        if (hasSuggestions() && (keyCode == 264 || keyCode == 265)) {
            suggestIndex = Math.max(0, suggestIndex + (keyCode == 264 ? 1 : -1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void saveAndClose() {
        String type = typeBox.getValue().trim();
        if (type.isEmpty()) return;
        ModMessages.sendToServer(new C2S_SaveNpcSettingsPacket(
                type, movement, attacksPlayers, allowRetaliation, invulnerable, lookMode));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, "NPC設定", cx, 16, 0xFFFFFF);
        g.drawString(this.font, "モデル", cx - FIELD_W / 2, 49, 0xC0C0C0, false);

        super.render(g, mouseX, mouseY, partialTick);

        if (hasSuggestions()) {
            int x = typeBox.getX(), y = typeBox.getY() + FIELD_H + 1;
            int w = typeBox.getWidth();
            int n = Math.min(MAX_SUGGEST, suggestions.size());
            g.fill(x, y, x + w, y + n * 11 + 2, 0xE0000000);
            for (int i = 0; i < n; i++) {
                int idx = (suggestIndex + i) % suggestions.size();
                g.drawString(this.font, suggestions.get(idx), x + 3, y + 2 + i * 11,
                        i == 0 ? 0xFFFFA0 : 0xB0B0B0, false);
            }
        }

        g.drawCenteredString(this.font,
                "Tabで候補を補完 / 保存後に右クリックで召喚・エンティティに右クリックでNPC化",
                cx, this.height - 12, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
