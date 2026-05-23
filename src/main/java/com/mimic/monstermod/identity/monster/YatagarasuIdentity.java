package com.mimic.monstermod.identity.monster;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.identity.monster.yatagarasu.*;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class YatagarasuIdentity extends BaseIdentity {
    public static final String MODID = "monstermod";

    // スキルIDをここで管理（HUDや他のクラスから参照しやすくするため）
    public static final SkillId ONIBI = new SkillId(new ResourceLocation(MODID, "bullet_hell"));
    public static final SkillId SPIRAL = new SkillId(new ResourceLocation(MODID, "bullet_ring"));
    public static final SkillId TORNADO = new SkillId(new ResourceLocation(MODID, "tornado"));
    // GUI/HUDで使用するスキルの並び順
    private static final SkillId[] SKILLS = {
            ONIBI,
            SPIRAL,
            TORNADO
    };

    // 各スキルのデフォルトクールダウン（tick）
    private static final int[] COOLDOWNS = {
            200, // Bullet Hell: 10秒
            200,
            200
    };

    public YatagarasuIdentity(@Nullable BaseEntity entity) {
        super(entity, SKILLS.length);
        this.skillIds = SKILLS;
        this.defaultCooldowns = COOLDOWNS;
    }

    public static void initSkillRegistry() {
        // BulletHellSkillクラスに定義したLeadを登録
        SkillLeadRegistry.register(OnibiSkill.createLead(ONIBI));
        SkillLeadRegistry.register(OnibiSpiralSkill.createLead(SPIRAL));
        SkillLeadRegistry.register(TornadoSkill.createLead(TORNADO));
    }

    @Override
    public CompoundTag serializeNBT() {
        return super.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
    }
}