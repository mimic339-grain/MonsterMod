package com.mimic.monstermod.skill;

import java.util.Collections;
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

    /* Server 用取得（未登録は例外） */
    public static SkillLead getStrict(SkillId id) {
        SkillLead lead = REGISTRY.get(id);
        if (lead == null) throw new IllegalStateException("SkillLead not registered: " + id);
        return lead;
    }

    /* Client 用取得（未登録は null） */
    public static SkillLead getNullable(SkillId id) {
        return REGISTRY.get(id);
    }

    public static boolean contains(SkillId id) {
        return REGISTRY.containsKey(id);
    }

    public static Map<SkillId, SkillLead> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static void validateNotEmpty() {
        if (REGISTRY.isEmpty()) throw new IllegalStateException(
                "SkillLeadRegistry is empty. Did you forget to register SkillLeads?"
        );
    }
}
