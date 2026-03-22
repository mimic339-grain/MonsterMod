package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.AttackExecutor;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.identity.impl.MimicIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_PlayerRootPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public final class SkillUtil {
    private static final Map<LivingEntity, Map<UUID, ActiveSkill>> ACTIVE_SKILLS = new HashMap<>();

    public static void tick(ServerLevel level) {
        if (ACTIVE_SKILLS.isEmpty()) return;

        for (Iterator<Map.Entry<LivingEntity, Map<UUID, ActiveSkill>>> it = ACTIVE_SKILLS.entrySet().iterator(); it.hasNext();) {
            Map.Entry<LivingEntity, Map<UUID, ActiveSkill>> casterEntry = it.next();
            LivingEntity caster = casterEntry.getKey();

            if (caster.level() != level) continue;

            Map<UUID, ActiveSkill> skills = casterEntry.getValue();

            skills.values().removeIf(active -> {
                active.ticksLeft--;

                if (active.lead.autoRoot && active.ticksLeft == active.lead.rootTickBeforeDamage) {
                    rootCaster(caster, true, active.lead.rootTickBeforeDamage);
                }

                if (active.ticksLeft <= 0) {
                    System.out.println("[SkillUtil] 最終攻撃実行: " + active.lead.id);
                    executeOnce(level, caster, active);

                    if (active.lead.autoRoot) {
                        rootCaster(caster, false, 0);
                    }
                    return true;
                }

                return false;
            });

            if (skills.isEmpty()) it.remove();
        }
    }

    public static void execute(ServerLevel level, Entity casterEntity, SkillLead lead, MathMain math) {
        if (!(casterEntity instanceof ServerPlayer player)) return;

        ACTIVE_SKILLS.remove((LivingEntity) player);

        int duration = Math.max(1, lead.totalPreviewTicks);
        System.out.println("[SkillUtil] スキル予約: " + lead.id + " | 待ち時間: " + duration + " ticks");

        SkillAttackSpec spec;
        try {
            spec = SkillAttackRegistry.getStrict(lead.skillId());
        } catch (Exception e) {
            spec = new SkillAttackSpec();
        }

        ActiveSkill active = new ActiveSkill(lead, math, spec, duration);
        ACTIVE_SKILLS.computeIfAbsent(player, k -> new HashMap<>()).put(UUID.randomUUID(), active);
    }

    private static void executeOnce(ServerLevel level, LivingEntity caster, ActiveSkill active) {
        // 1. ヒット判定とダメージ処理
        if (active.lead.attackType != AttackType.NONE) {
            Collection<LivingEntity> targets = AttackExecutor.collect(level, active.lead, active.math, caster);
            System.out.println("=== 範囲攻撃実行: " + active.lead.id + " | ヒット数: " + targets.size() + " ===");

            for (LivingEntity target : targets) {
                if (target == caster) continue;
                if (!active.hitTargets.add(target)) continue;

                active.spec.apply(caster, target);
                System.out.println("-> [命中] " + target.getName().getString());
            }
        }

        // 2. ★ クールダウンの正式開始処理
        if (caster instanceof Player player) {
            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                var identity = trans.getIdentity();
                if (identity instanceof MimicIdentity mimic) {
                    int index = mimic.findSkillIndex(active.lead.skillId());
                    if (index != -1) {
                        mimic.startActualCooldown(index);
                    }
                }
            });
        }
    }

    private static void rootCaster(LivingEntity caster, boolean root, int durationTicks) {
        if (!(caster instanceof ServerPlayer sp)) return;
        ModMessages.sendToPlayer(new S2C_PlayerRootPacket(sp.getUUID(), root ? durationTicks : 0), sp);
    }

    private static final class ActiveSkill {
        final SkillLead lead;
        final MathMain math;
        final SkillAttackSpec spec;
        final Set<LivingEntity> hitTargets = new HashSet<>();
        int ticksLeft;

        ActiveSkill(SkillLead lead, MathMain math, SkillAttackSpec spec, int ticksLeft) {
            this.lead = lead; this.math = math; this.spec = spec; this.ticksLeft = ticksLeft;
        }
    }
}