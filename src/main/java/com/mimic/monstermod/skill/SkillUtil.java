package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.AttackExecutor;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.identity.impl.MimicIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_PlayerRootPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SkillUtil {
    // 複数のスキル（コンボ）を同時に扱えるよう List で管理
    private static final Map<LivingEntity, List<ActiveSkill>> CURRENT_ACTIVE = new ConcurrentHashMap<>();

    public static void tick(ServerLevel level) {
        if (CURRENT_ACTIVE.isEmpty()) return;

        Iterator<Map.Entry<LivingEntity, List<ActiveSkill>>> it = CURRENT_ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, List<ActiveSkill>> entry = it.next();
            LivingEntity caster = entry.getKey();
            List<ActiveSkill> activeList = entry.getValue();

            if (caster.level() != level) continue;

            // 各スキルのカウントダウン
            activeList.removeIf(active -> {
                active.ticksLeft--;

                // Root(移動不能)の制御
                if (active.lead.autoRoot && active.ticksLeft == active.lead.rootTickBeforeDamage) {
                    rootCaster(caster, true, active.lead.rootTickBeforeDamage);
                }

                // スキル完了（攻撃実行）
                if (active.ticksLeft <= 0) {
                    System.out.println("[SkillUtil] 最終攻撃実行: " + active.lead.id);
                    executeOnce(level, caster, active);
                    if (active.lead.autoRoot) rootCaster(caster, false, 0);
                    return true; // リストから削除
                }
                return false;
            });

            if (activeList.isEmpty()) it.remove();
        }
    }

    public static void execute(ServerLevel level, ServerPlayer player, SkillLead lead, MathMain math) {
        List<ActiveSkill> activeList = CURRENT_ACTIVE.computeIfAbsent(player, k -> new CopyOnWriteArrayList<>());

        // 1. 緊急回避スキルの場合：現在の全スキルを強制終了
        if (lead.category == AttackType.Category.EMERGENCY) {
            System.out.println("[SkillUtil] 緊急キャンセル発動: " + lead.id);
            activeList.clear();
            rootCaster(player, false, 0);
        }
        // 2. 実行中のスキルがある場合
        else if (!activeList.isEmpty()) {
            // コンボスキルの場合：同時発動を許可
            if (lead.category == AttackType.Category.COMBO) {
                System.out.println("[SkillUtil] コンボ接続 (同時実行): " + lead.id);
            } else {
                // 通常スキルの場合：★「実行中のNORMALスキル」がある時だけ上書き禁止にする
                boolean hasNormalActive = activeList.stream().anyMatch(a -> a.lead.category == AttackType.Category.NORMAL);
                if (hasNormalActive) {
                    System.out.println("[SkillUtil] 通常スキル実行中のため無視 (待機): " + lead.id);
                    return;
                }
            }
        }

        int duration = Math.max(1, lead.totalPreviewTicks);
        SkillAttackSpec spec = SkillAttackRegistry.getStrict(lead.skillId());
        ActiveSkill active = new ActiveSkill(lead, math, spec, duration);

        System.out.println("[SkillUtil] スキル登録完了: " + lead.id + " | Category: " + lead.category + " | Duration: " + duration);
        activeList.add(active);
    }

    private static void executeOnce(ServerLevel level, LivingEntity caster, ActiveSkill active) {
        // ★ 修正: 攻撃タイプが STRIKE (旧ENTITY_AOE) か TOUCH の時だけ範囲判定を行う
        if (active.lead.attackType == AttackType.STRIKE || active.lead.attackType == AttackType.TOUCH) {
            Collection<LivingEntity> targets = AttackExecutor.collect(level, active.lead, active.math, caster);
            System.out.println("=== 攻撃判定実行: " + active.lead.id + " | ヒット数: " + targets.size() + " ===");
            for (LivingEntity target : targets) {
                if (target == caster) continue;
                active.spec.apply(caster, target);
            }
        } else {
            // 攻撃以外のスキル（DODGE, MOVEMENT等）は何もしない
            System.out.println("[SkillUtil] 非攻撃スキルの完了処理: " + active.lead.id);
        }

        // クールダウン開始の共通処理
        if (caster instanceof Player player) {
            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                if (trans.getIdentity() instanceof MimicIdentity mimic) {
                    int index = mimic.findSkillIndex(active.lead.skillId());
                    if (index != -1) mimic.startActualCooldown(index);
                }
            });
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
        final SkillAttackSpec spec;
        int ticksLeft;
        ActiveSkill(SkillLead lead, MathMain math, SkillAttackSpec spec, int ticksLeft) {
            this.lead = lead; this.math = math; this.spec = spec; this.ticksLeft = ticksLeft;
        }
    }
}