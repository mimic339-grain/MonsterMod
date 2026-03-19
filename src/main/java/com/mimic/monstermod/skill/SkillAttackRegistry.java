package com.mimic.monstermod.skill;

import java.util.HashMap;
import java.util.Map;

public final class SkillAttackRegistry {

    private static final Map<SkillId, SkillAttackSpec> MAP = new HashMap<>();

    private SkillAttackRegistry() {}

    /* ======================
     * 登録
     * ====================== */

    public static void register(SkillId id, SkillAttackSpec spec) {
        if (id == null || spec == null) {
            throw new NullPointerException("SkillId / SkillAttackSpec cannot be null");
        }
        MAP.put(id, spec);
    }

    /* ======================
     * Server 用（厳格）
     * ====================== */

    public static SkillAttackSpec getStrict(SkillId id) {
        SkillAttackSpec spec = MAP.get(id);
        if (spec == null) {
            throw new IllegalStateException("AttackSpec not found: " + id);
        }
        return spec;
    }

    /* ======================
     * 共通 / Client 許容
     * ====================== */

    public static SkillAttackSpec getNullable(SkillId id) {
        return MAP.get(id);
    }
}
