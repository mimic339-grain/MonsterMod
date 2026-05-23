package com.mimic.monstermod.variable.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IPlayerData{
    // 既存の定数
    String STATE_HIDE_FOOD = "hide_food_gauge";
    String STATE_HIDE_ARMOR = "hide_armor_gauge";

    // ★ 追加: プレビュー表示設定の定数
    String STATE_SHOW_SKILL_LEAD = "show_skill_lead";

    boolean hasState(String stateKey);
    void setState(String stateKey, boolean active);

    // ★ 追加: RGBカラーの設定
    void setLeadColor(float r, float g, float b);
    float getLeadR();
    float getLeadG();
    float getLeadB();

    void setLeadThickness(float thickness);
    float getLeadThickness();

    void tick(Player player);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag compoundTag);
}