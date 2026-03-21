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
            Map<UUID, ActiveSkill> skills = casterEntry.getValue();

            skills.values().removeIf(active -> {
                active.ticksLeft--;

                // --- Root 開始判定 ---
                if (active.lead.autoRoot && active.ticksLeft == active.lead.rootTickBeforeDamage) {
                    System.out.println("[ROOT DEBUG] Root conditions met for: " + caster.getName().getString());
                    rootCaster(caster, true, active.lead.rootTickBeforeDamage);
                }

                // --- ダメージ実行 ---
                if (active.ticksLeft == 0) {
                    System.out.println("[DEBUG] Damage Execution at 0 ticks");
                    executeOnce(level, caster, active);

                    // ★重要: ダメージが出た瞬間に移動制限を強制解除（0ティックで上書き送信）
                    if (active.lead.autoRoot) {
                        rootCaster(caster, false, 0);
                        System.out.println("[ROOT DEBUG] Force Unroot on Damage: " + caster.getName().getString());
                    }
                }

                return active.ticksLeft <= -1;
            });
            if (skills.isEmpty()) it.remove();
        }
    }

    public static void execute(ServerLevel level, Entity casterEntity, SkillLead lead, MathMain math) {
        System.out.println("[DEBUG] SkillUtil.execute ENTERED for: " + lead.id);

        if (!(casterEntity instanceof ServerPlayer player)) {
            System.out.println("[DEBUG] Execute FAILED: Caster is not ServerPlayer");
            return;
        }

        // 1. 登録準備（プレビュー時間を取得。設定がなければ3秒=60tick）
        int duration = lead.totalPreviewTicks > 0 ? lead.totalPreviewTicks : 60;
        SkillAttackSpec spec;

        // 2. レジストリ取得 (失敗時はダミーを匿名クラスで作成)
        try {
            spec = SkillAttackRegistry.getStrict(lead.skillId());
        } catch (Exception e) {
            System.out.println("[ERROR] SkillAttackRegistry not found for " + lead.id + ". Using dummy spec.");
            spec = new SkillAttackSpec() {
                @Override
                public void apply(LivingEntity attacker, LivingEntity target) {}
            };
        }

        // 3. 登録
        ActiveSkill active = new ActiveSkill(lead, math, spec, duration);
        ACTIVE_SKILLS.computeIfAbsent(player, k -> new HashMap<>()).put(UUID.randomUUID(), active);
        System.out.println("[DEBUG] ActiveSkill REGISTERED SUCCESS: " + lead.id + " (Duration: " + duration + ")");
    }

    private static void executeOnce(ServerLevel level, LivingEntity caster, ActiveSkill active) {
        Collection<LivingEntity> targets;
        switch (active.lead.attackType) {
            case ENTITY_AOE -> targets = AttackExecutor.collect(level, active.math);
            default -> targets = List.of();
        }

        for (LivingEntity target : targets) {
            if (!active.hitTargets.add(target)) continue;
            active.spec.apply(caster, target);
            System.out.println("[DEBUG] Damage applied to " + target.getName().getString());
        }
    }

    private static void rootCaster(LivingEntity caster, boolean root, int durationTicks) {
        if (!(caster instanceof ServerPlayer sp)) return;
        // root=false の場合は 0 を送る
        int ticks = root ? durationTicks : 0;
        ModMessages.sendToPlayer(new S2C_PlayerRootPacket(sp.getUUID(), ticks), sp);
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