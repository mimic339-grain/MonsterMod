package com.mimic.monstermod.variable.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;

public class MonsterData extends EntityData implements IMonsterData {

    private final List<String> fixedSkills = new ArrayList<>();
    private String skill = "";
    private int skillTick = 0;

    // スキルごとの残りクールタイム
    private final List<Integer> abilityCooldowns = new ArrayList<>();

    // 固定クールタイム（スキルごとに変化）
    private final List<Integer> skillCooldowns = new ArrayList<>();

    private int remainingHostilityTime = 0;

    public MonsterData(Entity owner) {
        super(owner);
    }

    @Override
    public List<String> getFixedSkills() { return fixedSkills; }

    @Override
    public void setFixedSkills(List<String> skills) {
        fixedSkills.clear();
        if (skills != null) fixedSkills.addAll(skills);

        // クールタイムリスト初期化
        abilityCooldowns.clear();
        skillCooldowns.clear();
        for (int i = 0; i < fixedSkills.size(); i++) {
            abilityCooldowns.add(0);       // 残りクールタイム初期化
            skillCooldowns.add(20);       // 固定クールタイムデフォルト
        }
        markDirty();
    }

    @Override
    public String getSkill() { return skill; }

    @Override
    public void setSkill(String skill) { this.skill = skill; markDirty(); }

    @Override
    public int getSkillTick() { return skillTick; }

    @Override
    public void setSkillTick(int tick) { this.skillTick = tick; markDirty(); }

    @Override
    public boolean isSkillFinished() { return skill.isEmpty(); }

    @Override
    public String selectSkill() {
        for (int i = 0; i < fixedSkills.size(); i++) {
            if (isActionReady(fixedSkills.get(i))) return fixedSkills.get(i);
        }
        return null;
    }

    @Override
    public void tick() {
        // スキル進行処理
        if (!skill.isEmpty()) skillTick++;

        // クールダウン減算
        for (int i = 0; i < abilityCooldowns.size(); i++) {
            int cd = abilityCooldowns.get(i);
            if (cd > 0) abilityCooldowns.set(i, cd - 1);
        }

        if (remainingHostilityTime > 0) remainingHostilityTime--;
    }

    @Override
    public boolean isActionReady(String skillName) {
        int index = fixedSkills.indexOf(skillName);
        if (index == -1) return false;
        return abilityCooldowns.get(index) <= 0;
    }

    // -----------------------------
    // スキルごとのクールタイム
    // -----------------------------
    @Override
    public int getSkillCooldown(int index) {
        if (index < 0 || index >= skillCooldowns.size()) return 20;
        return skillCooldowns.get(index);
    }

    @Override
    public int getAbilityCooldown(int index) {
        if (index < 0 || index >= abilityCooldowns.size()) return 0;
        return abilityCooldowns.get(index);
    }

    @Override
    public void setAbilityCooldown(int index, int cd) {
        if (index < 0 || index >= abilityCooldowns.size()) return;
        abilityCooldowns.set(index, cd);
        markDirty();
    }

    @Override
    public int getRemainingHostilityTime() { return remainingHostilityTime; }

    @Override
    public void setRemainingHostilityTime(int time) { this.remainingHostilityTime = time; markDirty(); }

    private boolean isTransformed = false;
    @Override
    public boolean isTransformed() { return isTransformed; }
    @Override
    public void setTransformed(boolean transformed) { this.isTransformed = transformed; markDirty(); }

    private String transformedMobId;
    @Override
    public String getTransformedMobId() { return transformedMobId; }
    @Override
    public void setTransformedMobId(String mobId) { this.transformedMobId = mobId; markDirty(); }

    // -----------------------------
    // NBT保存
    // -----------------------------
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("skill", skill);
        tag.putInt("skillTick", skillTick);
        tag.putIntArray("abilityCooldowns", abilityCooldowns.stream().mapToInt(i -> i).toArray());
        tag.putIntArray("skillCooldowns", skillCooldowns.stream().mapToInt(i -> i).toArray());
        tag.putInt("remainingHostilityTime", remainingHostilityTime);
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("transformedMobId", transformedMobId != null ? transformedMobId : "");
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        skill = tag.getString("skill");
        skillTick = tag.getInt("skillTick");
        remainingHostilityTime = tag.getInt("remainingHostilityTime");
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("transformedMobId") ? tag.getString("transformedMobId") : null;

        abilityCooldowns.clear();
        skillCooldowns.clear();
        int[] acs = tag.getIntArray("abilityCooldowns");
        int[] scs = tag.getIntArray("skillCooldowns");
        for (int i = 0; i < acs.length; i++) abilityCooldowns.add(acs[i]);
        for (int i = 0; i < scs.length; i++) skillCooldowns.add(scs[i]);
    }
}
