package com.mimic.monstermod.skill;

import java.util.HashMap;
import java.util.Map;

/**
 * SkillLeadRegistry
 *
 * 【役割】
 * ・SkillId → SkillLead の設計定義レジストリ
 * ・Client では nullable get を許可
 */
public final class SkillLeadRegistry {

    private static final Map<SkillId, SkillLead> REGISTRY = new HashMap<>();

    private SkillLeadRegistry() {}

    public static void register(SkillLead lead) {
        if (lead == null) throw new NullPointerException("SkillLead cannot be null");
        SkillId id = lead.skillId();
        if (REGISTRY.containsKey(id))
            throw new IllegalStateException("Duplicate SkillLead registration: " + id);
        REGISTRY.put(id, lead);
    }

    /* Client 用取得（未登録は null） */
    public static SkillLead getNullable(SkillId id) {
        return REGISTRY.get(id);
    }

    public static boolean contains(SkillId id) {
        return REGISTRY.containsKey(id);
    }
}
