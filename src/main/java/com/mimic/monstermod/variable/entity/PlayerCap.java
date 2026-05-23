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

    // ★ カラー用変数 (デフォルト赤)
    private float leadR = 1.0f;
    private float leadG = 0.0f;
    private float leadB = 0.0f;
    private float leadThickness = 3.0f; // ★ デフォルト太さ
    public PlayerCap(Entity owner) {
        this.owner = owner;
        this.activeStates.add(STATE_SHOW_SKILL_LEAD);
    }

    @Override
    public boolean hasState(String stateKey) { return activeStates.contains(stateKey); }

    @Override
    public void setState(String stateKey, boolean active) {
        boolean changed = active ? activeStates.add(stateKey) : activeStates.remove(stateKey);
        if (changed && owner instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }

    // ★ カラー関連の実装
    @Override public void setLeadColor(float r, float g, float b) {
        this.leadR = r; this.leadG = g; this.leadB = b;
        sync();
    }

    @Override public void setLeadThickness(float thickness) {
        this.leadThickness = thickness;
        sync();
    }

    @Override public float getLeadR() { return leadR; }
    @Override public float getLeadG() { return leadG; }
    @Override public float getLeadB() { return leadB; }
    @Override public float getLeadThickness() { return leadThickness; }
    private void sync() {
        if (owner instanceof ServerPlayer sp) CapabilityRegistry.syncToClient(sp);
    }
    @Override public void tick(Player player) {}

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag statesList = new ListTag();
        for (String state : activeStates) {
            statesList.add(StringTag.valueOf(state));
        }
        nbt.put("ActiveStates", statesList);
        // ★ カラーを保存
        nbt.putFloat("leadR", leadR);
        nbt.putFloat("leadG", leadG);
        nbt.putFloat("leadB", leadB);
        nbt.putFloat("leadThickness", leadThickness);
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
        // ★ カラーを読み込み (データがない場合はデフォルト赤)
        this.leadR = nbt.contains("leadR") ? nbt.getFloat("leadR") : 1.0f;
        this.leadG = nbt.getFloat("leadG");
        this.leadB = nbt.getFloat("leadB");
        this.leadThickness = nbt.contains("leadThickness") ? nbt.getFloat("leadThickness") : 3.0f;
    }
}