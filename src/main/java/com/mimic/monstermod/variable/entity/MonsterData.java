package com.mimic.monstermod.variable.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;

public class MonsterData extends EntityData implements IMonsterData {

    // 固定スキルリスト
    private final List<String> fixedSkills = new ArrayList<>();

    // 現在のスキル
    private String skill = "";

    // スキルの進行状態（tick数）
    private int skillTick = 0;

    // 能力クールダウン時間
    private int abilityCooldown = 0;

    // 残りホスティリティ時間
    private int remainingHostilityTime = 0;

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
        // スキルの進行処理
        if (!skill.isEmpty()) {
            skillTick++;
        }

        // クールダウンやホスティリティ時間の減算
        if (abilityCooldown > 0) {
            abilityCooldown--;
        }
        if (remainingHostilityTime > 0) {
            remainingHostilityTime--;
        }
    }

    // ----------------------------
    // 判定補助
    // ----------------------------
    @Override
    public boolean isActionReady(String skillName) {
        // クールダウン判定
        if (abilityCooldown > 0) return false;

        // 必要ならスキル名に応じた追加条件を追加
        return true;
    }

    // ----------------------------
    // クールダウンとホスティリティ時間のゲッター・セッター
    // ----------------------------
    @Override
    public int getAbilityCooldown() {
        return abilityCooldown;
    }

    @Override
    public void setAbilityCooldown(int cooldown) {
        this.abilityCooldown = cooldown;
        markDirty();
    }

    @Override
    public int getRemainingHostilityTime() {
        return remainingHostilityTime;
    }

    @Override
    public void setRemainingHostilityTime(int time) {
        this.remainingHostilityTime = time;
        markDirty();
    }

    // ----------------------------
    // 変身状態の管理
    // ----------------------------
    private boolean isTransformed = false;

    @Override
    public boolean isTransformed() {
        return isTransformed;
    }

    @Override
    public void setTransformed(boolean transformed) {
        this.isTransformed = transformed;
        markDirty();
    }

    // 変身後のモンスターID取得
    private String transformedMobId;

    @Override
    public String getTransformedMobId() {
        return transformedMobId;
    }

    @Override
    public void setTransformedMobId(String mobId) {
        this.transformedMobId = mobId;
        markDirty();
    }

    // ----------------------------
    // NBT保存処理
    // ----------------------------
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("skill", skill);
        tag.putInt("skillTick", skillTick);
        tag.putInt("abilityCooldown", abilityCooldown);
        tag.putInt("remainingHostilityTime", remainingHostilityTime);
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("transformedMobId", transformedMobId != null ? transformedMobId : "");
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.skill = tag.getString("skill");
        this.skillTick = tag.getInt("skillTick");
        this.abilityCooldown = tag.getInt("abilityCooldown");
        this.remainingHostilityTime = tag.getInt("remainingHostilityTime");
        this.isTransformed = tag.getBoolean("isTransformed");
        this.transformedMobId = tag.contains("transformedMobId") ? tag.getString("transformedMobId") : null;
    }
}
