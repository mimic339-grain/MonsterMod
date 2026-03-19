package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.AttackExecutor;
import com.mimic.monstermod.Math.MathMain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class ServerSkillExecutor {

    /**
     * caster → (instanceId → ActiveSkill)
     */
    private static final Map<
            LivingEntity,
            Map<UUID, ActiveSkill>
            > ACTIVE_SKILLS = new HashMap<>();

    private ServerSkillExecutor() {}

    /* ======================
     * Tick（唯一の実行点）
     * ====================== */
    public static void tick(ServerLevel level) {

        for (Iterator<Map.Entry<LivingEntity, Map<UUID, ActiveSkill>>> it =
             ACTIVE_SKILLS.entrySet().iterator(); it.hasNext();) {

            Map.Entry<LivingEntity, Map<UUID, ActiveSkill>> casterEntry = it.next();

            LivingEntity caster = casterEntry.getKey();
            Map<UUID, ActiveSkill> skills = casterEntry.getValue();

            skills.values().removeIf(active -> {

                executeOnce(level, caster, active);

                return --active.ticksLeft <= 0;
            });

            if (skills.isEmpty()) {
                it.remove();
            }
        }
    }

    /* ======================
     * Execute（登録のみ）
     * ====================== */
    public static void execute(
            ServerLevel level,
            Entity casterEntity,
            SkillLead lead,
            MathMain math
    ) {

        if (!(casterEntity instanceof LivingEntity caster)) return;
        if (lead.attackType == AttackType.NONE) return;

        SkillAttackSpec spec =
                SkillAttackRegistry.getStrict(lead.skillId());

        int ticks = Math.max(lead.lifetimeTick, 1);

        ActiveSkill active = new ActiveSkill(
                lead,
                math,
                spec,
                ticks
        );

        // ★ 修正ポイント：UUIDで完全独立インスタンス化
        ACTIVE_SKILLS
                .computeIfAbsent(caster, k -> new HashMap<>())
                .put(UUID.randomUUID(), active);
    }

    /* ======================
     * 1 Tick 実行
     * ====================== */
    private static void executeOnce(
            ServerLevel level,
            LivingEntity caster,
            ActiveSkill active
    ) {

        MathMain math = active.math;
        SkillAttackSpec spec = active.spec;

        Collection<LivingEntity> targets;

        switch (active.lead.attackType) {

            case ENTITY_AOE -> {
                targets = AttackExecutor.collect(level, math);
            }

            case BLOCK_AOE -> {
                // 将来用（今は空）
                targets = List.of();
            }

            case NONE -> {
                targets = List.of();
            }

            default -> {
                targets = List.of();
            }
        }

        for (LivingEntity target : targets) {

            // ★ 多段ヒット制御（1回のみ）
            if (!active.hitTargets.add(target)) continue;

            spec.apply(caster, target);
        }
    }

    /* ======================
     * ActiveSkill
     * ====================== */
    private static final class ActiveSkill {

        final SkillLead lead;
        final MathMain math;
        final SkillAttackSpec spec;

        /**
         * 既にヒットした対象
         */
        final Set<LivingEntity> hitTargets = new HashSet<>();

        int ticksLeft;

        ActiveSkill(
                SkillLead lead,
                MathMain math,
                SkillAttackSpec spec,
                int ticksLeft
        ) {
            this.lead = lead;
            this.math = math;
            this.spec = spec;
            this.ticksLeft = ticksLeft;
        }
    }
}