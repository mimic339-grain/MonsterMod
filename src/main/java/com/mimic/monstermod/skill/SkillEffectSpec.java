package com.mimic.monstermod.skill;

import net.minecraft.world.entity.LivingEntity;
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
        applyToCaster(attacker);
        if (target != null && target.isAlive()) {
            applyToTarget(attacker, target);
        }
    }

    protected void applyToCaster(LivingEntity attacker) {
        // ★ ここに書いてあった「真後ろに15ブロックワープ」の処理を削除またはコメントアウト！
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