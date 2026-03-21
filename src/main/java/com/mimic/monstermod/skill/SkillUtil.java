package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.AttackExecutor;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_PlayerRootPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class SkillUtil {
    private static final Map<LivingEntity, Map<UUID, ActiveSkill>> ACTIVE_SKILLS = new HashMap<>();

    public static void tick(ServerLevel level) {
        if (ACTIVE_SKILLS.isEmpty()) return;

        for (Iterator<Map.Entry<LivingEntity, Map<UUID, ActiveSkill>>> it = ACTIVE_SKILLS.entrySet().iterator(); it.hasNext();) {
            Map.Entry<LivingEntity, Map<UUID, ActiveSkill>> casterEntry = it.next();
            LivingEntity caster = casterEntry.getKey();

            // ★重要: キャスターが今処理中のディメンションにいない場合はスキップ（2重進捗防止）
            if (caster.level() != level) continue;

            Map<UUID, ActiveSkill> skills = casterEntry.getValue();

            skills.values().removeIf(active -> {
                active.ticksLeft--;

                // --- Root 開始判定 ---
                // 残り時間が設定値(デフォルト10)になった瞬間、クライアントへ停止命令を送る
                if (active.lead.autoRoot && active.ticksLeft == active.lead.rootTickBeforeDamage) {
                    rootCaster(caster, true, active.lead.rootTickBeforeDamage);
                }

                // --- ダメージ実行 ---
                if (active.ticksLeft <= 0) {
                    executeOnce(level, caster, active);

                    // ダメージと同時に移動制限を解除
                    if (active.lead.autoRoot) {
                        rootCaster(caster, false, 0);
                    }
                    return true; // このtickで削除
                }

                return false;
            });

            if (skills.isEmpty()) it.remove();
        }
    }

    public static void execute(ServerLevel level, Entity casterEntity, SkillLead lead, MathMain math) {
        if (!(casterEntity instanceof ServerPlayer player)) return;

        // ★上書き対策: 同じプレイヤーの古いスキル（プレビュー）を一旦クリア
        ACTIVE_SKILLS.remove(player);

        int duration = lead.totalPreviewTicks > 0 ? lead.totalPreviewTicks : 60;
        SkillAttackSpec spec;

        try {
            spec = SkillAttackRegistry.getStrict(lead.skillId());
        } catch (Exception e) {
            spec = new SkillAttackSpec() {
                @Override
                public void apply(LivingEntity attacker, LivingEntity target) {}
            };
        }

        ActiveSkill active = new ActiveSkill(lead, math, spec, duration);
        ACTIVE_SKILLS.computeIfAbsent(player, k -> new HashMap<>()).put(UUID.randomUUID(), active);
    }

    private static void executeOnce(ServerLevel level, LivingEntity caster, ActiveSkill active) {
        // プレビューが 2D でも 3D でも Block でも、ENTITY_AOE なら一律で 3D 判定を実行
        // もし attackType が NONE でないなら必ず判定するように条件を緩めます
        if (active.lead.attackType == AttackType.NONE) return;

        // AttackExecutor に自分自身 (caster) を渡して除外させる
        Collection<LivingEntity> targets = AttackExecutor.collect(level, active.math, caster);

        System.out.println("=== AOE EXECUTION: " + active.lead.id + " ===");

        for (LivingEntity target : targets) {
            if (!active.hitTargets.add(target)) continue;

            // 実際のダメージ処理へ
            active.spec.apply(caster, target);
            System.out.println("-> [HIT] " + target.getName().getString());
        }
    }

    private static void rootCaster(LivingEntity caster, boolean root, int durationTicks) {
        if (!(caster instanceof ServerPlayer sp)) return;
        // durationTicksを送るが、クライアント側では「0になるまで止まる」フラグとして使う
        ModMessages.sendToPlayer(new S2C_PlayerRootPacket
                (sp.getUUID(), root ? durationTicks : 0), sp);
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