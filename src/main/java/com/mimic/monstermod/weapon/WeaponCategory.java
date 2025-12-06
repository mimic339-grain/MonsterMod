package com.mimic.monstermod.weapon;

import net.minecraft.world.item.ItemStack;

public enum WeaponCategory {
    NONE("none", 0f, 0, new float[]{0f,0f,0f}, 0f, 0, "none"),
    HAMMER("hammer", 12.0f, 3, new float[]{14f,18f,22f}, 0.25f, 3, "hammer"),
    AXE("axe", 10.0f, 3, new float[]{10f,12f,14f}, 0.18f, 3, "axe"),
    DUAL("dual_blade", 4.0f, 3, new float[]{6f,6f,6f}, 0.05f, 3, "dualblade"),
    LONGSWORD("longsword", 8.0f, 3, new float[]{10f,12f,15f}, 0.15f, 3, "longsword"),
    GREAT_SWORD("greatsword", 14.0f, 3, new float[]{12f,16f,20f}, 0.20f, 3, "greatsword");

    private final String id;
    private final float baseDamage;
    private final int maxCombo;
    /** comboごとの硬直（seconds or ticks depending on convention — you decide unit） */
    private final float[] comboStiffness;
    private final float movePenalty;
    private final int skillSlots;
    private final String layerName;

    WeaponCategory(String id, float baseDamage, int maxCombo, float[] comboStiffness, float movePenalty, int skillSlots, String layerName) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.maxCombo = maxCombo;
        this.comboStiffness = comboStiffness;
        this.movePenalty = movePenalty;
        this.skillSlots = skillSlots;
        this.layerName = layerName;
    }

    public String getId() { return id; }
    public float getBaseDamage() { return baseDamage; }
    public int getMaxCombo() { return maxCombo; }
    public float getMovePenalty() { return movePenalty; }
    public int getSkillSlots() { return skillSlots; }
    public String getLayerName() { return layerName; }

    /** comboStage は 0-based（0 = 1段目）を想定。範囲外なら最後の値を返す */
    public float getAttackStiffness(int comboStage) {
        if (comboStiffness == null || comboStiffness.length == 0) return 0f;
        if (comboStage < 0) comboStage = 0;
        if (comboStage >= comboStiffness.length) comboStage = comboStiffness.length - 1;
        return comboStiffness[comboStage];
    }

    /** 必要なら ItemStack を受け取りカテゴリ固有の補正ダメージを返すためのフック */
    public float getDamageForStage(ItemStack stack, int comboStage) {
        // デフォルトは baseDamage。個別の ItemStack の NBT やタグで上書きする場合はここに実装を追加
        // 例えば comboStage による倍率を入れるなら：
        float multiplier = 1.0f + comboStage * 0.2f; // 例: 1.0, 1.2, 1.4
        return baseDamage * multiplier;
    }
}
