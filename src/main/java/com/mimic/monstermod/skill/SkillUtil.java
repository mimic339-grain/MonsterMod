package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.AttackExecutor;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_PlayerRootPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SkillUtil {
    private static final Map<LivingEntity, List<ActiveSkill>> CURRENT_ACTIVE = new ConcurrentHashMap<>();

    /**
     * スキル実行を試行する。条件に合わない場合は false を返す。
     */

    public static boolean tryExecute(ServerLevel level, ServerPlayer player, SkillLead lead, MathMain math) {
        List<ActiveSkill> activeList = CURRENT_ACTIVE.computeIfAbsent(player, k -> new CopyOnWriteArrayList<>());

        // NORMALカテゴリのスキルが動作中（skillTicksが残っている）かチェック
        boolean isDoingNormal = activeList.stream()
                .anyMatch(a -> a.lead.category == SkillType.Category.NORMAL && a.ticksLeft > 0);

        // --- 条件判定 ---

        // 1. EMERGENCY: 既存スキルを全てクリアして強制実行
        if (lead.category == SkillType.Category.EMERGENCY) {
            System.out.println("[SkillUtil] EMERGENCYキャンセル実行: " + lead.id + " (既存スキル " + activeList.size() + " 件を破棄)");
            activeList.clear();
            rootCaster(player, false, 0);
        }

        // 2. NORMAL: 他のNORMALが動いているなら拒否
        else if (lead.category == SkillType.Category.NORMAL && isDoingNormal) {
            System.out.println("[SkillUtil] 実行拒否: 他の NORMAL スキルが動作中: " + lead.id);
            return false;
        }

        // 3. COMBO: isDoingNormal に関係なくここを通過する

        // --- 実行登録 ---
        int duration = lead.skillTicks;
        SkillEffectSpec spec = SkillEffectRegistry.getStrict(lead.skillId());

        ActiveSkill active = new ActiveSkill(lead, math, spec, duration);
        activeList.add(active);

        System.out.println("[SkillUtil] スキル登録成功: " + lead.id + " (Category: " + lead.category + ", Ticks: " + duration + ")");
        return true;
    }

    public static void tick(ServerLevel level) {
        if (CURRENT_ACTIVE.isEmpty()) return;

        Iterator<Map.Entry<LivingEntity, List<ActiveSkill>>> it = CURRENT_ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, List<ActiveSkill>> entry = it.next();
            LivingEntity caster = entry.getKey();
            List<ActiveSkill> activeList = entry.getValue();

            if (caster.level() != level) continue;

            activeList.removeIf(active -> {
                active.ticksLeft--;

                // Root(移動不能)の制御
                if (active.lead.autoRoot && active.ticksLeft == active.lead.rootTickBeforeDamage) {
                    rootCaster(caster, true, active.lead.rootTickBeforeDamage);
                }

                if (active.ticksLeft <= 0) {
                    System.out.println("[SkillUtil/Debug] 予兆完了。エフェクト実行: " + active.lead.id);
                    executeFinalEffect(level, caster, active);

                    if (active.lead.autoRoot) rootCaster(caster, false, 0);

                    // ★ [修正] ここで identity.startActualCooldown を呼ぶ必要がなくなりました
                    return true;
                }
                return false;
            });

            if (activeList.isEmpty()) it.remove();
        }
    }

    private static void executeFinalEffect(ServerLevel level, LivingEntity caster, ActiveSkill active) {
        // 1. 攻撃系 (STRIKE / TOUCH) の処理
        if (active.lead.skillType == SkillType.STRIKE || active.lead.skillType == SkillType.TOUCH) {
            Collection<LivingEntity> targets = AttackExecutor.collect(level, active.lead, active.math, caster);
            System.out.println("[SkillUtil/Debug] 攻撃判定: " + active.lead.id + " | ヒット数: " + targets.size());
            for (LivingEntity target : targets) {
                if (target == caster) continue;
                active.spec.apply(caster, target);
            }
        }

        // ★ 2. 移動・特殊系 (MOVEMENT) の処理を追加！
        // これがないと、回避ワープが実行されません。
        else if (active.lead.skillType == SkillType.MOVEMENT) {
            System.out.println("[SkillUtil/Debug] 特殊効果(MOVEMENT)実行: " + active.lead.id);
            // ターゲットなし(null)で実行することで、applyToCaster が呼ばれる
            active.spec.apply(caster, null);
        }
    }

    private static void rootCaster(LivingEntity caster, boolean root, int durationTicks) {
        if (caster instanceof ServerPlayer sp) {
            ModMessages.sendToPlayer(new S2C_PlayerRootPacket(sp.getUUID(), root ? durationTicks : 0), sp);
        }
    }

    private static final class ActiveSkill {
        final SkillLead lead;
        final MathMain math;
        final SkillEffectSpec spec;
        int ticksLeft;
        ActiveSkill(SkillLead lead, MathMain math, SkillEffectSpec spec, int ticksLeft) {
            this.lead = lead; this.math = math; this.spec = spec; this.ticksLeft = ticksLeft;
        }
    }
}