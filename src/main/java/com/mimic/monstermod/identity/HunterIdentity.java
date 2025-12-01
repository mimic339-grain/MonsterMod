package com.mimic.monstermod.identity;

import net.minecraft.nbt.CompoundTag;

/**
 * HunterIdentity
 * -----------------------------------------------------------
 * プレイヤーごとの「ハンター専用状態」を保持するクラス。
 * Capability の内部データで使用され、サーバーとクライアント間で同期される。
 * 保存される情報：
 *  - 武器カテゴリ（weaponCategory）
 *  - スキルスロット設定（dodge / skill1 / skill2 / skill3 / sheathe）
 *  - メニューで現在選択中のスロット（menuSlot）
 * 想定用途：
 *  - GUI でスキル選択
 *  - 武器カテゴリごとのスキル割り当て
 *  - 戦闘時のスキル入力処理
 * 設計方針：
 *  - 大規模マルチプレイに耐える軽量・高速構造
 *  - NBT 保存・同期しやすい
 */
public class HunterIdentity {

    // ------------------------------------------------------------
    // 基本ステータス
    // ------------------------------------------------------------
    /** 現在変身しているかどうか */
    private boolean isTransformed = false;

    /** 現在使用している武器カテゴリ（例："katana"） */
    private String weaponCategory = "none";

    // ------------------------------------------------------------
    // スキルスロット
    // ------------------------------------------------------------

    /** 回避・緊急回避スキル */
    private String dodgeSkill = null;

    /** 通常スキル1 */
    private String skill1 = null;

    /** 通常スキル2 */
    private String skill2 = null;

    /** 通常スキル3 */
    private String skill3 = null;

    /** 納刀中スキル（特別枠） */
    private String sheatheSkill = null;

    // ------------------------------------------------------------
    // GUI メニュー用
    // ------------------------------------------------------------

    /**
     * 現在 GUI で選択中のスロット。
     *
     * 例：
     *  "dodge"
     *  "skill1"
     *  "skill2"
     *  "skill3"
     *  "sheathe"
     *  "none"
     */
    private String menuSlot = "none";
    /**
     * 武器カテゴリを変更する。
     * カテゴリが変わった場合はスキルが不整合になるため全てリセット。
     */
    public void setWeaponCategory(String category) {
        if (!category.equals(this.weaponCategory)) {
            this.weaponCategory = category;
            resetSkills();
        }
    }

    public String getDodgeSkill() { return dodgeSkill; }
    public String getSkill1() { return skill1; }
    public String getSkill2() { return skill2; }
    public String getSkill3() { return skill3; }
    public String getSheatheSkill() { return sheatheSkill; }

    public void setDodgeSkill(String id) { this.dodgeSkill = id; }
    public void setSkill1(String id) { this.skill1 = id; }
    public void setSkill2(String id) { this.skill2 = id; }
    public void setSkill3(String id) { this.skill3 = id; }
    public void setSheatheSkill(String id) { this.sheatheSkill = id; }

    // ------------------------------------------------------------
    // メニュー選択中スロット
    // ------------------------------------------------------------

    public String getMenuSlot() {
        return menuSlot;
    }

    public void setMenuSlot(String slot) {
        this.menuSlot = slot;
    }

    // ============================================================
    // ユーティリティ
    // ============================================================

    /**
     * 武器カテゴリ変更時、または初期化時に呼ぶ。
     * 全スキルを初期化して不整合を解消。
     */
    public void resetSkills() {
        this.dodgeSkill = null;
        this.skill1 = null;
        this.skill2 = null;
        this.skill3 = null;
        this.sheatheSkill = null;
    }

    // ============================================================
    // NBT セーブ / ロード
    // ============================================================

    /**
     * この Identity の内容を NBT に書き出す。
     */
    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("IsTransformed", isTransformed);
        tag.putString("WeaponCategory", weaponCategory);

        // 各スキル（null の場合は保存しない）
        if (dodgeSkill != null) tag.putString("DodgeSkill", dodgeSkill);
        if (skill1 != null) tag.putString("Skill1", skill1);
        if (skill2 != null) tag.putString("Skill2", skill2);
        if (skill3 != null) tag.putString("Skill3", skill3);
        if (sheatheSkill != null) tag.putString("SheatheSkill", sheatheSkill);

        // 現在の GUI 選択スロット
        tag.putString("MenuSlot", menuSlot);

        return tag;
    }

    /**
     * NBT から Identity の内容を復元する。
     */
    public void loadNBT(CompoundTag tag) {
        this.isTransformed = tag.getBoolean("IsTransformed");
        this.weaponCategory = tag.getString("WeaponCategory");

        this.dodgeSkill = tag.contains("DodgeSkill") ? tag.getString("DodgeSkill") : null;
        this.skill1     = tag.contains("Skill1")     ? tag.getString("Skill1")     : null;
        this.skill2     = tag.contains("Skill2")     ? tag.getString("Skill2")     : null;
        this.skill3     = tag.contains("Skill3")     ? tag.getString("Skill3")     : null;
        this.sheatheSkill = tag.contains("SheatheSkill") ? tag.getString("SheatheSkill") : null;

        // メニューの選択位置（デフォルトは none）
        this.menuSlot = tag.contains("MenuSlot") ? tag.getString("MenuSlot") : "none";
    }
}
