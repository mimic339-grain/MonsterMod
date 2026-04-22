package com.mimic.monstermod.variable.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IPlayerData{
    String STATE_HIDE_FOOD = "hide_food_gauge";
    String STATE_HIDE_ARMOR = "hide_armor_gauge";
    // これさえあれば、どんなスキルの状態も管理できる
    boolean hasState(String stateKey);
    void setState(String stateKey, boolean active);

    // もし「硬化」以外のバニラに近い処理（落下無効など）を
    // 他でも使い回すなら残してもいいですが、基本はStateで代用可能です。
    void tick(Player player);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag compoundTag);
}