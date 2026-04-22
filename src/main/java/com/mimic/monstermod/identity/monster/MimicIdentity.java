package com.mimic.monstermod.identity.monster;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.identity.util.MimicSkillLeads;
import com.mimic.monstermod.skill.SkillId;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class MimicIdentity extends BaseIdentity {

    private static final SkillId[] SKILLS = {
            MimicSkillLeads.TEST_2D,
            MimicSkillLeads.TEST_BLOCK,
            MimicSkillLeads.TEST_3D,
            MimicSkillLeads.TEST_EMERGENCY,
    };

    private static final int[] COOLDOWNS = {120, 140, 180, 210};

    public MimicIdentity(@Nullable BaseEntity entity) {
        // 親クラスで SKILLS.length 分の abilityCooldowns と lockCooldowns が new される
        super(entity, SKILLS.length);
        this.skillIds = SKILLS;
        this.defaultCooldowns = COOLDOWNS;
        // ここで this.abilityCooldowns = new int[...] をしてはいけない（親の結果を消してしまうため）
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