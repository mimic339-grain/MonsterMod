package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.util.MimicSkillLeads;
import com.mimic.monstermod.skill.SkillId;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class MimicIdentity extends BaseMonsterIdentity {

    private static final SkillId[] SKILLS = {
            MimicSkillLeads.TEST_2D,
            MimicSkillLeads.TEST_BLOCK,
            MimicSkillLeads.TEST_3D,
            MimicSkillLeads.TEST_EMERGENCY,
    };

    private static final int[] COOLDOWNS = {120, 140, 180, 210};

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILLS.length);
        this.skillIds = SKILLS;
        this.defaultCooldowns = COOLDOWNS;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        if (getEntity() instanceof MimicEntity mimic) {
            tag.putBoolean("isOpen", mimic.isOpen());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        if (getEntity() instanceof MimicEntity mimic && tag.contains("isOpen")) {
            mimic.setOpen(tag.getBoolean("isOpen"));
        }
    }
}