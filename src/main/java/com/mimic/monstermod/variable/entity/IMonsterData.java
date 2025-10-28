package com.mimic.monstermod.variable.entity;

import net.minecraft.nbt.CompoundTag;
import java.util.List;

public interface IMonsterData {
    List<String> getFixedSkills();
    void setFixedSkills(List<String> skills);

    String getSkill();
    void setSkill(String skill);

    int getSkillTick();
    void setSkillTick(int tick);
    boolean isSkillFinished();

    String selectSkill();

    void tick();

    boolean isActionReady(String skillName);

    int getSkillCooldown(int index);
    int getAbilityCooldown(int index);
    void setAbilityCooldown(int index, int cd);

    int getRemainingHostilityTime();
    void setRemainingHostilityTime(int time);

    boolean isTransformed();
    void setTransformed(boolean transformed);

    String getTransformedMobId();
    void setTransformedMobId(String mobId);

    // -----------------------------
    // NBT保存・復元
    // -----------------------------
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}
