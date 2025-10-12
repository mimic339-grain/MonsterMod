package com.mimic.monstermod.variable.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface IEntityData {

    // --- Scale / Visuals ---
    void setScale(float scale);
    float getScale();

    // --- Flags ---
    void setFlag(String key, boolean value);
    boolean getFlag(String key);

    // --- Cooldowns / Action readiness ---
    void setCooldown(String action, long availableTick);
    boolean isActionReady(String action);

    // --- Player-specific scrolls / skill counts ---
    void setMainScroll(int value);
    int getMainScroll();
    void alterMainScroll(int delta);

    void setDGKScroll(int value);
    int getDGKScroll();
    void alterDGKScroll(int delta);

    // --- Sync ---
    void syncToClient(ServerPlayer player);
    void syncIfChanged();

    // --- Serialization ---
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    // --- Utility ---
    Entity getOwner();
    boolean isValid();
}
