package com.mimic.monstermod.variable.entity;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * EntityDataを継承せず、単体で動作するCapability実装
 */
public class PlayerCap implements IPlayerData, INBTSerializable<CompoundTag> {

    private final Set<String> activeStates = new HashSet<>();
    private final Entity owner;

    public PlayerCap(Entity owner) {
        this.owner = owner;
    }

    @Override
    public boolean hasState(String stateKey) {
        return activeStates.contains(stateKey);
    }

    @Override
    public void setState(String stateKey, boolean active) {
        boolean changed = active ? activeStates.add(stateKey) : activeStates.remove(stateKey);

        // サーバー側で値が変わったら即座に同期
        if (changed && owner instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }

    @Override
    public void tick(Player player) {
        // 必要になったらここにロジックを書く
    }

    // --- NBT保存 (super呼び出しを削除) ---

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag statesList = new ListTag();
        for (String state : activeStates) {
            statesList.add(StringTag.valueOf(state));
        }
        nbt.put("ActiveStates", statesList);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.activeStates.clear();
        if (nbt.contains("ActiveStates", Tag.TAG_LIST)) {
            ListTag statesList = nbt.getList("ActiveStates", Tag.TAG_STRING);
            for (int i = 0; i < statesList.size(); i++) {
                activeStates.add(statesList.getString(i));
            }
        }
    }
}