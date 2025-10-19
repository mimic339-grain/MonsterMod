package com.mimic.monstermod.variable.entity;

import java.util.List;

public interface IMonsterData extends IEntityData {

    // 固定スキルリスト
    List<String> getFixedSkills();
    void setFixedSkills(List<String> skills);

    // 現在のスキル
    String getSkill();
    void setSkill(String skill);

    // スキル進行用 Tick
    int getSkillTick();
    void setSkillTick(int tick);

    // スキル完了判定
    boolean isSkillFinished();

    // 実行するスキルを選択
    String selectSkill();

    // Tick処理（AIやクールダウン更新など）
    void tick();

    // ======================
    // 変身状態に関連する新しいメソッド
    // ======================

    // 能力クールダウン
    int getAbilityCooldown();
    void setAbilityCooldown(int cooldown);

    // 残りホスティリティ時間
    int getRemainingHostilityTime();
    void setRemainingHostilityTime(int time);

    // 現在の変身状態の確認
    boolean isTransformed();
    void setTransformed(boolean transformed);

    // 変身後のモンスターID取得
    String getTransformedMobId();
    void setTransformedMobId(String mobId);
}
