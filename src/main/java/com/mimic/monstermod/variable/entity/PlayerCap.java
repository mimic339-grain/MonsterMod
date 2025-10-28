package com.mimic.monstermod.variable.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerCap extends EntityData implements IPlayerData {

    private final List<String> skillSlots = new ArrayList<>(3);
    private final Map<Integer, String> keybindMap = new HashMap<>();

    public PlayerCap(Entity owner) {
        super(owner);
        // 初期化
        for (int i = 0; i < 3; i++) skillSlots.add("");
    }

    @Override
    public List<String> getSkillSlots() {
        return skillSlots;
    }

    @Override
    public void setSkillSlot(int index, String skillId) {
        if (index >= 0 && index < skillSlots.size()) {
            skillSlots.set(index, skillId);
            markDirty();
        }
    }

    @Override
    public Map<Integer, String> getKeybindMap() {
        return keybindMap;
    }

    @Override
    public void tick() {
        // クールダウン更新などは EntityData 側で対応可能
    }

    // -----------------------------
    // NBT保存
    // -----------------------------
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < skillSlots.size(); i++) {
            tag.putString("skillSlot" + i, skillSlots.get(i));
        }
        keybindMap.forEach((key, skillId) -> tag.putString("keybind_" + key, skillId));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // スキルスロット復元
        for (int i = 0; i < skillSlots.size(); i++) {
            skillSlots.set(i, tag.getString("skillSlot" + i));
        }
        // キーバインド復元
        keybindMap.clear();
        tag.getAllKeys().forEach(key -> {
            if (key.startsWith("keybind_")) {
                int k = Integer.parseInt(key.substring(8));
                keybindMap.put(k, tag.getString(key));
            }
        });
    }

    /**
     * 2引数版 deserializeNBT
     * CapabilityRegistry など呼び出し側が Level を渡せるように
     */
    public void deserializeNBT(CompoundTag tag, Level level) {
        deserializeNBT(tag); // 従来の処理に委譲
        // ここに level を使った処理を追加する場合は今後拡張可能
    }
}
