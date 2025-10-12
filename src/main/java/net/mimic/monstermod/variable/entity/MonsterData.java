package net.mimic.monstermod.variable.entity;

import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.List;

public class MonsterData extends EntityData implements IMonsterData {

    private final List<String> fixedSkills = new ArrayList<>();
    private String skill = "";
    private int skillTick = 0;

    public MonsterData(Entity owner) {
        super(owner);
    }

    // ----------------------------
    // 固定スキル管理
    // ----------------------------
    @Override
    public List<String> getFixedSkills() {
        return fixedSkills;
    }

    @Override
    public void setFixedSkills(List<String> skills) {
        fixedSkills.clear();
        if (skills != null) fixedSkills.addAll(skills);
        markDirty();
    }

    // ----------------------------
    // 現在のスキル管理
    // ----------------------------
    @Override
    public String getSkill() {
        return skill;
    }

    @Override
    public void setSkill(String skill) {
        this.skill = skill;
        markDirty();
    }

    @Override
    public int getSkillTick() {
        return skillTick;
    }

    @Override
    public void setSkillTick(int tick) {
        this.skillTick = tick;
        markDirty();
    }

    @Override
    public boolean isSkillFinished() {
        return skill.isEmpty();
    }

    // ----------------------------
    // スキル選択
    // ----------------------------
    @Override
    public String selectSkill() {
        for (String s : fixedSkills) {
            if (isActionReady(s)) return s;
        }
        return null;
    }

    // ----------------------------
    // Tick 処理（サーバー側）
    // ----------------------------
    @Override
    public void tick() {
        if (!skill.isEmpty()) skillTick++;
        // 必要ならクールダウンやフラグ処理を追加
    }

    // ----------------------------
    // 判定補助
    // ----------------------------
    public boolean isActionReady(String skillName) {
        // TODO: クールダウン判定など
        return true;
    }
}
