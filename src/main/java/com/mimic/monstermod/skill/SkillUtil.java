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


    public static boolean tryExecute(ServerLevel level, ServerPlayer player, SkillLead lead, MathMain math) {
        List<ActiveSkill> activeList = CURRENT_ACTIVE.computeIfAbsent(player, k -> new CopyOnWriteArrayList<>());

        // 現在 UNIQUE スキルが実行中か？
        boolean isUniqueRunning = activeList.stream().anyMatch(a -> a.lead.category == SkillType.Category.UNIQUE);
        // 現在「コンボ受付中」のスキルがあるか？
        boolean hasComboWindow = activeList.stream().anyMatch(a -> a.comboWindowTicks > 0);

        boolean canExecute = false;

        if (lead.category == SkillType.Category.CANCEL) {
            activeList.clear();
            rootCaster(player, false, 0);
            canExecute = true;
        }
        else if (lead.category == SkillType.Category.COMBO) {
            if (hasComboWindow) {
                canExecute = true;
                // 前のスキルの硬直を解除
                activeList.forEach(a -> a.rootDisabled = true);
                rootCaster(player, false, 0);
            }
        }
        else if (lead.category == SkillType.Category.NORMAL) {
            // UNIQUE中ではなく、かつ (何もしていない OR DASH中)
            boolean isOnlyDashing = activeList.isEmpty() || activeList.stream().allMatch(a -> a.lead.category == SkillType.Category.DASH);
            if (!isUniqueRunning && isOnlyDashing) canExecute = true;
        }
        else { // UNIQUE, DASH
            if (activeList.isEmpty()) canExecute = true;
        }

        if (!canExecute) return false;

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
                if (active.comboWindowTicks > 0) active.comboWindowTicks--;

                int recoveryStart = active.lead.recoveryTicks;
                int effectStart = recoveryStart + active.lead.effectTicks;
                int preRootStart = effectStart + active.lead.beforeRecoverTicks;

                // --- Root制御: !active.rootDisabled が立っている場合のみ rootCaster を呼ぶ ---

                // 1. 前硬直
                if (!active.rootDisabled && active.lead.autoRoot && active.ticksLeft == preRootStart) {
                    rootCaster(caster, true, active.lead.beforeRecoverTicks);
                }

                // 2. 攻撃開始
                if (active.ticksLeft == effectStart) {
                    executeFinalEffect(level, caster, active); // 攻撃はフラグに関係なく必ず出す

                    if (!active.rootDisabled && active.lead.autoRoot) {
                        if (!active.lead.canMoveDuringEffect) {
                            rootCaster(caster, true, active.lead.effectTicks);
                        } else {
                            rootCaster(caster, false, 0);
                        }
                    }
                }

                // 3. 後隙開始
                if (!active.rootDisabled && active.lead.autoRoot && active.ticksLeft == recoveryStart) {
                    if (active.lead.recoveryTicks > 0) {
                        rootCaster(caster, true, active.lead.recoveryTicks);
                    }
                }

                // 4. 終了
                if (active.ticksLeft <= 0) {
                    // コンボされていなければ（通常終了）、最後にRootを解除
                    if (!active.rootDisabled && active.lead.autoRoot) rootCaster(caster, false, 0);
                    return true;
                }
                return false;
            });

            if (activeList.isEmpty()) it.remove();
        }
    }
    private static final class ActiveSkill {
        final SkillLead lead;
        final MathMain math;
        final SkillEffectSpec spec;
        int ticksLeft;
        int comboWindowTicks;
        boolean rootDisabled = false; // コンボ発動時に true に書き換えられる

        ActiveSkill(SkillLead lead, MathMain math, SkillEffectSpec spec, int ticksLeft) {
            this.lead = lead;
            this.math = math;
            this.spec = spec;
            this.ticksLeft = ticksLeft;
            this.comboWindowTicks = lead.comboWindowTicks;
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

}