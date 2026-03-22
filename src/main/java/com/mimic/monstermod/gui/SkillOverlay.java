package com.mimic.monstermod.gui;

public class SkillOverlay {
//    public static final IGuiOverlay HUD_SKILLS = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) -> {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null || mc.screen != null) return;
//
//        CapabilityRegistry.getPlayerTransformation(mc.player).ifPresent(trans -> {
//            if (!trans.isTransformed()) return;
//
//            BaseMonsterIdentity identity = trans.getIdentity();
//            if (identity == null) return;
//
//            SkillId[] skills = identity.getSkills();
//            int skillCount = skills.length;
//            if (skillCount == 0) return;
//
//            // --- レイアウト定数 (右側寄せ) ---
//            int spacing = 22;
//            int xRightEdge = (width / 2) + 95;
//            int baseY = height - 22;
//
//            for (int i = 0; i < skillCount; i++) {
//                int row = 0;
//                int col = i;
//
//                if (skillCount >= 7) {
//                    if (i < 6) {
//                        row = 0;
//                        col = i;
//                    } else {
//                        row = 1;
//                        col = i - 6;
//                    }
//                }
//
//                int drawX = xRightEdge + (col * spacing);
//                int drawY = baseY - (row * spacing);
//
//                renderSlot(guiGraphics, drawX, drawY, i, identity, mc.font, (skillCount >= 7));
//            }
//        });
//    };
//
//    private static void renderSlot(GuiGraphics gui, int x, int y, int index, BaseMonsterIdentity identity, Font font, boolean isTwoRows) {
//        SkillId skillId = identity.getSkills()[index];
//        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
//
//        int cd = identity.getCooldown(index);
//        int maxCd = identity.getMaxCooldown(index);
//        int castingIdx = identity.getCastingIndex();
//
//        // --- 1. 枠色の決定 ---
//        int frameColor = 0xFF00FF00; // Ready: 緑
//        if (cd > 0) {
//            frameColor = 0xFFFF0000; // Cooldown: 赤
//        } else if (castingIdx != -1 && castingIdx != index) {
//            if (lead == null || lead.category != AttackType.Category.COMBO) {
//                frameColor = 0xFF555555;
//            }
//        }
//
//        // スロット枠と背景
//        gui.fill(x, y, x + 20, y + 20, frameColor);
//        gui.fill(x + 1, y + 1, x + 19, y + 19, 0xFF000000);
//
//        // クールダウンゲージ
//        if (cd > 0 && maxCd > 0) {
//            float ratio = Math.min(1.0f, (float) cd / maxCd);
//            int fillHeight = (int) (18 * ratio);
//            gui.fill(x + 1, y + 19 - fillHeight, x + 19, y + 19, 0xAA555555);
//        }
//
//        // --- 2. キーラベル (スロット中央) ---
//        String keyLabel = getBindingLabel(index);
//        gui.drawCenteredString(font, keyLabel, x + 10, y + 6, 0xFFFFFFFF);
//
//        // --- 3. スキル名の表示 (スロットの「すぐ上」に配置) ---
//        String path = skillId.location().getPath();
//        String name = path.replace("test_", "").toUpperCase(); // 2Dなどを強調するため大文字に
//
//        gui.pose().pushPose();
//
//        // 文字サイズを少しアップ (0.7f)
//        float scale = 0.7f;
//
//        // y - 8 にすることで、スロットの上端(y)の少し上に名前が来ます。
//        // scaleがかかるので座標計算に注意
//        float nameX = (x + 10) / scale;
//        float nameY = (y - 6) / scale;
//
//        gui.pose().scale(scale, scale, scale);
//
//        // 色を少し明るいグレー(0xFFDDDDDD)にして、読みやすく
//        gui.drawCenteredString(font, name, (int)nameX, (int)nameY, 0xFFDDDDDD);
//
//        gui.pose().popPose();
//    }
//
//    /**
//     * MonsterKeyBindings の設定から現在のキー表示名を取得する
//     */
//    private static String getBindingLabel(int index) {
//        // 設定された KeyMapping 配列から現在のキー名を取得
//        if (index >= 0 && index < MonsterKeyBindings.SKILL_KEYS.length) {
//            // 例: "R", "T", "Y"... ユーザーが設定で変えればここも変わる
//            return MonsterKeyBindings.SKILL_KEYS[index].getTranslatedKeyMessage().getString().toUpperCase();
//        }
//        return String.valueOf(index + 1);
//    }
}