package com.mimic.monstermod.skill;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

public class SkillEffectSpec {

    public final float damage;
    public final DamageType damageType; // 属性 (PHYSICALなど)
    public final SkillType skillType; // 挙動 (MOVEMENTなど)
    public final List<PotionEffectSpec> effects;

    // デフォルトコンストラクタ
    public SkillEffectSpec() {
        this.damage = 0.0f;
        this.damageType = DamageType.PHYSICAL;
        this.skillType = SkillType.STRIKE;
        this.effects = Collections.emptyList();
    }

    // ★ 修正: コンストラクタの引数を整理
    public SkillEffectSpec(float damage, DamageType damageType, SkillType skillType, List<PotionEffectSpec> effects) {
        this.damage = damage;
        this.damageType = damageType;
        this.skillType = skillType;
        this.effects = effects;
    }

    /**
     * スキルの最終実行（SkillUtilから呼ばれる）
     */
    public void apply(LivingEntity attacker, @Nullable LivingEntity target) {
        // 1. まず自分自身への効果を判定 (targetの有無に関わらず実行)
        applyToCaster(attacker);

        // 2. ターゲットがいる場合のみ、相手への効果を実行
        if (target != null && target.isAlive()) {
            applyToTarget(attacker, target);
        }
    }

    private void applyToCaster(LivingEntity attacker) {
        // AttackTypeがMOVEMENTの場合、ワープ処理を実行
        if (this.skillType == SkillType.MOVEMENT) {
            float yaw = attacker.getYRot();
            double rad = Math.toRadians(yaw);
            // 真後ろへ15ブロック移動
            Vec3 targetPos = attacker.position().add(-Math.sin(rad) * 15.0, 0, Math.cos(rad) * 15.0);

            // サーバー側での位置更新
            attacker.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            attacker.setDeltaMovement(Vec3.ZERO);

            System.out.println("[Skill/Debug] MOVEMENT(回避)実行: " + attacker.getName().getString());
        }
    }

    private void applyToTarget(LivingEntity attacker, LivingEntity target) {
        if (this.damage < 0) {
            // 回復
            target.heal(-this.damage);
            System.out.println("[SKILL-APPLY] Healed " + target.getName().getString());
        } else if (this.damage > 0) {
            // ダメージ (属性に応じたDamageSourceを生成)
            boolean hurt = target.hurt(damageType.create(attacker), this.damage);

            if (hurt) {
                double dx = attacker.getX() - target.getX();
                double dz = attacker.getZ() - target.getZ();
                target.knockback(0.5D, dx, dz);
                System.out.println("[SKILL-APPLY] Damaged " + target.getName().getString());
            }
        }

        // ステータス異常付与
        for (PotionEffectSpec effect : effects) {
            effect.apply(target);
        }
    }
}