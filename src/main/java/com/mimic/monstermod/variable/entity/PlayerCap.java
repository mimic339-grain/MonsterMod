package com.mimic.monstermod.variable.entity;

import net.minecraft.world.entity.Entity;
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
}
