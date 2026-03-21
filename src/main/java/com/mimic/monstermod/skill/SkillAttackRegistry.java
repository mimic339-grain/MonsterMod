package com.mimic.monstermod.skill;

import net.minecraft.world.effect.MobEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillAttackRegistry {

    private static final Map<SkillId, SkillAttackSpec> MAP = new HashMap<>();

    static {
        // ① test_2d: 純粋な物理ダメージ (5.0 = ハート2.5個)
        register(SkillId.of("monstermod", "test_2d"), new SkillAttackSpec(
                5.0f, DamageType.PHYSICAL, List.of()
        ));

        // ② test_3d: 魔法ダメージ(2.0) + 10秒間の毒
        register(SkillId.of("monstermod", "test_3d"), new SkillAttackSpec(
                2.0f, DamageType.MAGIC, List.of(
                new StatusEffectSpec(MobEffects.POISON, 200, 1) // 200ticks = 10秒
        )
        ));

        // ③ test_block: 体力回復 (10.0 = ハート5個)
        register(SkillId.of("monstermod", "test_block"), new SkillAttackSpec(
                -10.0f, DamageType.MAGIC, List.of()
        ));
    }

    private SkillAttackRegistry() {}

    public static void register(SkillId id, SkillAttackSpec spec) {
        if (id == null || spec == null) throw new NullPointerException("SkillId / SkillAttackSpec cannot be null");
        MAP.put(id, spec);
    }

    public static SkillAttackSpec getStrict(SkillId id) {
        SkillAttackSpec spec = MAP.get(id);
        if (spec == null) throw new IllegalStateException("AttackSpec not found: " + id);
        return spec;
    }

    public static SkillAttackSpec getNullable(SkillId id) {
        return MAP.get(id);
    }
}