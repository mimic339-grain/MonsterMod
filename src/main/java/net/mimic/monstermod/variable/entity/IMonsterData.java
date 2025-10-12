package net.mimic.monstermod.variable.entity;

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
}
