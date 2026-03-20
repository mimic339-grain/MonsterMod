package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.AttackExecutor;
import com.mimic.monstermod.Math.MathMain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class ServerSkillExecutor {

    private static final Map<LivingEntity, Map<UUID, ActiveSkill>> ACTIVE_SKILLS = new HashMap<>();

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

                // Root判定（ダメージ前）
                if (active.lead.autoRoot && active.ticksLeft == active.lead.rootTickBeforeDamage) {
                    rootCaster(caster, true);
                }

                // 1 Tick 実行（ダメージ判定）
                executeOnce(level, caster, active);

                // Tick 減算
                active.ticksLeft--;

                // Root解除（ダメージ後）
                if (active.lead.autoRoot && active.ticksLeft <= 0) {
                    rootCaster(caster, false);
                }

                return active.ticksLeft <= 0;
            });

            if (skills.isEmpty()) it.remove();
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

        SkillAttackSpec spec = SkillAttackRegistry.getStrict(lead.skillId());
        int ticks = Math.max(lead.lifetimeTick, 1);

        ActiveSkill active = new ActiveSkill(lead, math, spec, ticks);

        ACTIVE_SKILLS
                .computeIfAbsent(caster, k -> new HashMap<>())
                .put(UUID.randomUUID(), active);
    }

    /* ======================
     * 1 Tick 実行（ダメージ判定）
     * ====================== */
    private static void executeOnce(
            ServerLevel level,
            LivingEntity caster,
            ActiveSkill active
    ) {
        // ダメージは ticksLeft == 0 で発生
        if (active.ticksLeft != 0) return;

        Collection<LivingEntity> targets;
        switch (active.lead.attackType) {
            case ENTITY_AOE -> targets = AttackExecutor.collect(level, active.math);
            case BLOCK_AOE -> targets = List.of();
            case NONE -> targets = List.of();
            default -> targets = List.of();
        }

        for (LivingEntity target : targets) {
            if (!active.hitTargets.add(target)) continue; // 多段ヒット防止
            active.spec.apply(caster, target);
        }
    }

    /* ======================
     * プレイヤー Root
     * ====================== */
    private static void rootCaster(LivingEntity caster, boolean root) {
        //TODO 0.5秒間だけ持続して動けないようにする　鈍足を付与したり　個人的には位置の固定がいいんじゃないかなって思う　さーばーとクライアントで両方やるんでもいいけどとりあえず動けないようにしてくれたらいい　
    }

    /* ======================
     * ActiveSkill
     * ====================== */
    private static final class ActiveSkill {

        final SkillLead lead;
        final MathMain math;
        final SkillAttackSpec spec;
        final Set<LivingEntity> hitTargets = new HashSet<>();
        int ticksLeft;

        ActiveSkill(SkillLead lead, MathMain math, SkillAttackSpec spec, int ticksLeft) {
            this.lead = lead;
            this.math = math;
            this.spec = spec;
            this.ticksLeft = ticksLeft;
        }
    }
}