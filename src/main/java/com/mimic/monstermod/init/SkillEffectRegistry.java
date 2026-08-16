package com.mimic.monstermod.init;

import com.mimic.monstermod.effect.ModEffects;
import com.mimic.monstermod.identity.monster.YatagarasuIdentity;
import com.mimic.monstermod.identity.bomber.BomberSkills;
import com.mimic.monstermod.identity.monster.yatagarasu.*;
import com.mimic.monstermod.skill.*;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillEffectRegistry {
    private static final Map<SkillId, SkillEffectSpec> MAP = new HashMap<>();

    static {
        register(YatagarasuIdentity.ONIBI, new OnibiSkill());
        register(YatagarasuIdentity.SPIRAL, new OnibiSpiralSkill());
        register(YatagarasuIdentity.TORNADO, new TornadoSkill());

        // ボマー(見た目を変えない役職)の6スキル
        register(BomberSkills.TOUCH,  BomberSkills.touch());
        register(BomberSkills.ITEM,   BomberSkills.itemTrap());
        register(BomberSkills.BLOCK,  BomberSkills.blockTrap());
        register(BomberSkills.VANISH, BomberSkills.vanish());
        register(BomberSkills.PLACE,  BomberSkills.place());
        register(BomberSkills.RELAY,  BomberSkills.relay());
        // ① test_2d: 物理ダメージ + STRIKE挙動
        register(SkillId.of("monstermod", "test_2d"), new SkillEffectSpec(
                5.0f, DamageType.PHYSICAL, SkillType.STRIKE, List.of()
        ));

        // ② test_3d: 魔法ダメージ + 毒 + STRIKE挙動
//        register(SkillId.of("monstermod", "test_3d"), new SkillEffectSpec(
//                2.0f, DamageType.MAGIC, SkillType.STRIKE, List.of(
//                new PotionEffectSpec(MobEffects.POISON, 200, 1)
//        )
//        ));

        // ③ test_block: 体力回復 + STRIKE挙動 (回復も判定が必要なためSTRIKE)
        register(SkillId.of("monstermod", "test_block"), new SkillEffectSpec(
                -10.0f, DamageType.MAGIC, SkillType.STRIKE, List.of()
        ));
        register(SkillId.of("monstermod", "test_3d"), new SkillEffectSpec(
                2.0f, DamageType.MAGIC, SkillType.STRIKE, List.of(
                new PotionEffectSpec(MobEffects.POISON, 200, 1),
                new PotionEffectSpec(ModEffects.BIND.get(), 140, 0) // 140ticks = 7秒
        )
        ));
        // ④ test_emergency: 回避 (ダメージ0, 属性は何でも可, MOVEMENT挙動)
        register(SkillId.of("monstermod", "test_emergency"), new SkillEffectSpec(
                0.0f, DamageType.PHYSICAL, SkillType.MOVEMENT, List.of()
        ));
    }

    private SkillEffectRegistry() {}

    public static void register(SkillId id, SkillEffectSpec spec) {
        if (id == null || spec == null) throw new NullPointerException("SkillId / SkillAttackSpec cannot be null");
        MAP.put(id, spec);
    }

    public static SkillEffectSpec getStrict(SkillId id) {
        SkillEffectSpec spec = MAP.get(id);
        if (spec == null) throw new IllegalStateException("AttackSpec not found: " + id);
        return spec;
    }

    public static SkillEffectSpec getNullable(SkillId id) {
        return MAP.get(id);
    }
}