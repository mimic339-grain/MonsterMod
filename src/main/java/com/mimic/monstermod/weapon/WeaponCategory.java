package com.mimic.monstermod.weapon;

import java.util.List;

/**
 * WeaponCategory — 完全版
 * 目的:
 *  - 各武器カテゴリの基礎ステータスを一元管理する Enum
 *  - HunterCombatState / WeaponItem / GUI 等から参照される想定
 * 含まれる情報:
 *  - id: JSON / NBT / デバッグ用の識別文字列
 *  - baseDamage: カテゴリ基礎ダメージ（段階倍率と掛け合わせる）
 *  - attackRange: 攻撃判定の距離（汎用）
 *  - comboMax: 通常コンボの最大段数（例: 3）
 *  - comboDamageMultipliers: 各段のダメージ倍率（配列長は comboMax 推奨）
 *  - comboStiffnessFrames: 各段の硬直（tick 単位）
 *  - movePenalty: 抜刀時の移動倍率（例: 0.85f = 85%）
 *  - hpBonus: 抜刀時に適用する最大体力のボーナス（絶対値 or 割合は設計次第）
 *  - layerName: 表示用レイヤー / アニメ区分名
 *  - skillSlots: 装備可能なスキルスロット数（GUI と Capability 用）
 *  - allowedSkills: このカテゴリで使用可能な skill ID の一覧（String）
 *
 * 将来的な拡張:
 *  - カテゴリごとに JSON 定義ファイルでロードする設計にも移しやすい形
 */
public enum WeaponCategory {

    NONE(
            "none",
            0f,      // baseDamage
            2.0f,    // attackRange
            1,       // comboMax
            new float[]{1.0f},      // comboDamageMultipliers
            new int[]{6},           // comboStiffnessFrames (tick)
            1.0f,    // movePenalty (1.0 = no penalty)
            0f,      // hpBonus (absolute HP add)
            "none",
            List.of()
    ),

    HAMMER(
            "hammer",
            12.0f,
            3.2f,
            3,
            new float[]{1.0f, 1.25f, 1.5f},
            new int[]{18, 22, 28},
            0.85f,
            0f,
            "hammer_layer",
            List.of("hammer_spin", "hammer_charge", "hammer_smash")
    ),
    SWORD(
            "greatsword",
            15.0f,
            3.4f,
            3,
            new float[]{1.0f, 1.3f, 1.6f},
            new int[]{22, 28, 36},
            0.8f,
            4.0f, // example: +4 HP on draw (or interpreted by system)
            "greatsword_layer",
            List.of("gs_charge", "gs_upper", "gs_stab")
    );

    // -------------------------
    // fields
    // -------------------------
    private final String id;
    private final float baseDamage;
    private final float attackRange;
    private final int comboMax;
    private final float[] comboDamageMultipliers;
    private final int[] comboStiffnessFrames;
    private final float movePenalty;
    private final float hpBonus;
    private final String layerName;
    private final List<String> allowedSkills;

    WeaponCategory(
            String id,
            float baseDamage,
            float attackRange,
            int comboMax,
            float[] comboDamageMultipliers,
            int[] comboStiffnessFrames,
            float movePenalty,
            float hpBonus,
            String layerName,
            List<String> allowedSkills
    ) {
        this.id = id;
        this.baseDamage = baseDamage;
        this.attackRange = attackRange;
        this.comboMax = Math.max(1, comboMax);
        this.comboDamageMultipliers = comboDamageMultipliers == null ? new float[]{1.0f} : comboDamageMultipliers;
        this.comboStiffnessFrames = comboStiffnessFrames == null ? new int[]{6} : comboStiffnessFrames;
        this.movePenalty = movePenalty;
        this.hpBonus = hpBonus;
        this.layerName = layerName;
        this.allowedSkills = allowedSkills;
    }

    // -------------------------
    // getters used by other systems
    // -------------------------
    public String getId() { return id; }

    /** カテゴリ基礎ダメージ（この値にコンボ倍率などを乗算して最終ダメージを得る） */
    public float getBaseDamage() { return baseDamage; }

    /** 攻撃判定のレンジ（ワールド内判定に使う） */
    public float getAttackRange() { return attackRange; }

    /** このカテゴリの通常コンボ上限（段数） */
    public int getComboMax() { return comboMax; }

    /** 指定段のダメージ倍率を返す。stage は 0-based。範囲外は末尾 or 0 を返す */
    public float getDamageMultiplierForStage(int stage) {
        if (comboDamageMultipliers == null || comboDamageMultipliers.length == 0) return 1.0f;
        if (stage < 0) stage = 0;
        if (stage >= comboDamageMultipliers.length) stage = comboDamageMultipliers.length - 1;
        return comboDamageMultipliers[stage];
    }

    /** 指定段の硬直（tick 単位）。範囲外は末尾を返す */
    public int getStiffnessForStage(int stage) {
        if (comboStiffnessFrames == null || comboStiffnessFrames.length == 0) return 6;
        if (stage < 0) stage = 0;
        if (stage >= comboStiffnessFrames.length) stage = comboStiffnessFrames.length - 1;
        return comboStiffnessFrames[stage];
    }

    /** 抜刀時の移動倍率（1.0 = 通常） */
    public float getMovePenalty() { return movePenalty; }

    /** 抜刀時に加算する HP（解釈は設計次第。割合にしたければ別メソッドを追加） */
    public float getHpBonus() { return hpBonus; }

    /** レイヤー / アニメ分岐に使う名前 */
    public String getLayerName() { return layerName; }

    /** このカテゴリで使用可能なスキル ID 一覧 */
    public List<String> getAllowedSkills() { return allowedSkills; }

    /** スキル許可チェック */
    public boolean isSkillAllowed(String skillId) {
        if (skillId == null || allowedSkills == null) return false;
        return allowedSkills.contains(skillId);
    }

    // ユーティリティ: コンボ段の最終ダメージ（基礎 × 段倍率）
    public float getDamageForStage(int stage) {
        return getBaseDamage() * getDamageMultiplierForStage(stage);
    }
}
