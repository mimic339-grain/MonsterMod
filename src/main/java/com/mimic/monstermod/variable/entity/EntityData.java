package com.mimic.monstermod.variable.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 完全版 EntityData
 * - GeckoLib アニメーション管理を使うため古い AnimationState 関連を削除
 * - スケール・フラグ・クールダウン・スクロール管理のみ
 * - クライアント同期は必要な時に syncToClient / syncIfChanged を使用
 */
public class EntityData implements IEntityData {
    private final Entity owner;
    private boolean dirty = false;

    private float scale = 1.0f;
    private final Map<String, Boolean> flags = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    // Player-specific scrolls
    private int mainScroll = 0;
    private int dgkScroll = 0;

    public EntityData(Entity owner) {
        this.owner = owner;
    }

    // --- Scale / Flags ---
    @Override
    public void setScale(float scale) {
        if (this.scale != scale) {
            this.scale = scale;
            markDirty();
        }
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public void setFlag(String key, boolean value) {
        if (flags.getOrDefault(key, false) != value) {
            flags.put(key, value);
            markDirty();
        }
    }

    @Override
    public boolean getFlag(String key) {
        return flags.getOrDefault(key, false);
    }

    // --- Cooldowns ---
    @Override
    public void setCooldown(String action, long availableTick) {
        cooldowns.put(action, availableTick);
        markDirty();
    }

    @Override
    public boolean isActionReady(String action) {
        if (owner != null) {
            long tick = owner.getCommandSenderWorld().getGameTime();
            return cooldowns.getOrDefault(action, 0L) <= tick;
        }
        return true;
    }

    // --- Scrolls ---
    @Override
    public void setMainScroll(int value) {
        mainScroll = value;
        markDirty();
    }

    @Override
    public int getMainScroll() {
        return mainScroll;
    }

    @Override
    public void alterMainScroll(int delta) {
        setMainScroll(mainScroll + delta);
    }

    @Override
    public void setDGKScroll(int value) {
        dgkScroll = value;
        markDirty();
    }

    @Override
    public int getDGKScroll() {
        return dgkScroll;
    }

    @Override
    public void alterDGKScroll(int delta) {
        setDGKScroll(dgkScroll + delta);
    }

    // --- Sync ---
    @Override
    public void syncToClient(ServerPlayer player) {
        // TODO: S2CEntityDataSyncPacket を作って送信
        clearDirty();
    }

    @Override
    public void syncIfChanged() {
        if (dirty && owner instanceof ServerPlayer player) syncToClient(player);
    }

    protected void markDirty() {
        dirty = true;
    }

    protected void clearDirty() {
        dirty = false;
    }

    // --- Serialization ---
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putFloat("Scale", scale);

        CompoundTag flagTag = new CompoundTag();
        flags.forEach(flagTag::putBoolean);
        tag.put("Flags", flagTag);

        tag.putInt("MainScroll", mainScroll);
        tag.putInt("DGKScroll", dgkScroll);

        CompoundTag cdTag = new CompoundTag();
        cooldowns.forEach(cdTag::putLong);
        tag.put("Cooldowns", cdTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        scale = nbt.getFloat("Scale");

        flags.clear();
        if (nbt.contains("Flags")) {
            CompoundTag flagTag = nbt.getCompound("Flags");
            for (String key : flagTag.getAllKeys()) flags.put(key, flagTag.getBoolean(key));
        }

        mainScroll = nbt.getInt("MainScroll");
        dgkScroll = nbt.getInt("DGKScroll");

        cooldowns.clear();
        if (nbt.contains("Cooldowns")) {
            CompoundTag cdTag = nbt.getCompound("Cooldowns");
            for (String key : cdTag.getAllKeys()) cooldowns.put(key, cdTag.getLong(key));
        }

        clearDirty();
    }

    // --- Utility ---
    @Override
    public Entity getOwner() {
        return owner;
    }

    @Override
    public boolean isValid() {
        return owner != null && owner.isAlive();
    }
}
