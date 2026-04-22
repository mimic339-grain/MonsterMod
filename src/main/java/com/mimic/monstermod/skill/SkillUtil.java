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
                // --- タイムラインの考え方 ---
                // [登録時(skillTicks)] -> [プレビュー] -> [前硬直開始] -> [効果発生(effectStart)] -> [後隙開始(recoveryStart)] -> [終了(0)]

                // 1 tick 減算
                // 重要: プレビュー 0 の場合、登録時の ticksLeft は (0 + effect + recovery) となる。
                // 登録直後の最初の tick でここを通ると、ticksLeft は (effect + recovery) と一致し、即座に executeFinalEffect が呼ばれる。
                active.ticksLeft--;

                int recoveryStart = active.lead.recoveryTicks;
                int effectStart = recoveryStart + active.lead.effectTicks;
                int beforeRootStart = effectStart + active.lead.beforeRecoverTicks;

                // 1. 前硬直 (予兆の終盤に差し掛かったタイミング)
                // beforeRecoverTicks が 10 なら、効果発生の 10tick 前に root をかける
                if (!active.rootDisabled && active.lead.autoRoot && active.ticksLeft == beforeRootStart) {
                    rootCaster(caster, true, active.lead.beforeRecoverTicks);
                }

                // 2. 攻撃・効果発生 (プレビュー期間が終了した瞬間)
                // プレビュー 0 のスキルは、この判定を「登録から 1tick 目」で通過する
                if (active.ticksLeft == effectStart) {
                    executeFinalEffect(level, caster, active);

                    if (!active.rootDisabled && active.lead.autoRoot) {
                        // 効果時間中(effectTicks)の移動可否を判定
                        if (!active.lead.canMoveDuringEffect) {
                            rootCaster(caster, true, active.lead.effectTicks);
                        } else {
                            // 動ける場合は root を解除（上書き）
                            rootCaster(caster, false, 0);
                        }
                    }
                }
// 2.5 【追加】効果時間の終了判定 (ActiveTicks が終わった瞬間)
                // recoveryStart (後隙開始) と同じタイミングが「効果終了」の瞬間です
                if (active.ticksLeft == recoveryStart) {
                    if (caster instanceof ServerPlayer sp) {
                        com.mimic.monstermod.skill.hunter.HunterSkill hunterSkill =
                                com.mimic.monstermod.skill.hunter.HunterSkillRegistry.get(active.lead.skillId());
                        if (hunterSkill != null) {
                            hunterSkill.onEffectEnd(sp); // ここで飛行解除や無敵解除を行う
                            System.out.println("[SkillUtil/Debug] 効果終了(onEffectEnd): " + active.lead.id);
                        }
                    }
                }
                // 3. 後隙開始 (効果時間が終了し、recovery 期間に入る瞬間)
                if (!active.rootDisabled && active.lead.autoRoot && active.ticksLeft == recoveryStart) {
                    if (active.lead.recoveryTicks > 0) {
                        rootCaster(caster, true, active.lead.recoveryTicks);
                    }
                }

                // 4. 全行程終了
                if (active.ticksLeft <= 0) {
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

        // 2. 移動・特殊系 (MOVEMENT) の処理
        else if (active.lead.skillType == SkillType.MOVEMENT) {
            System.out.println("[SkillUtil/Debug] 特殊効果(MOVEMENT)実行: " + active.lead.id);
            // JSON側のEffectSpecを実行
            active.spec.apply(caster, null);

            // ★【追加】HunterSkill クラスの applyEffect (Javaコード側) を実行
            if (caster instanceof ServerPlayer sp) {
                com.mimic.monstermod.skill.hunter.HunterSkill hunterSkill =
                        com.mimic.monstermod.skill.hunter.HunterSkillRegistry.get(active.lead.skillId());
                if (hunterSkill != null) {
                    hunterSkill.applyEffect(sp);
                }
            }
        }
    }

    private static void rootCaster(LivingEntity caster, boolean root, int durationTicks) {
        if (caster instanceof ServerPlayer sp) {
            ModMessages.sendToPlayer(new S2C_PlayerRootPacket(sp.getUUID(), root ? durationTicks : 0), sp);
        }
    }

}