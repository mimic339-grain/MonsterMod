package com.mimic.monstermod.skill;

import net.minecraft.world.entity.LivingEntity;
import java.util.Collections;
import java.util.List;

public class SkillAttackSpec {

    public final float damage;
    public final DamageType type;
    public final List<StatusEffectSpec> effects;

    // ダミー作成用
    public SkillAttackSpec() {
        this.damage = 0.0f;
        this.type = DamageType.PHYSICAL;
        this.effects = Collections.emptyList();
    }

    public SkillAttackSpec(float damage, DamageType type, List<StatusEffectSpec> effects) {
        this.damage = damage;
        this.type = type;
        this.effects = effects;
    }

    /**
     * 実効果適用（ダメージ・回復・ノックバック・エフェクト）
     */
    public void apply(LivingEntity attacker, LivingEntity target) {
        if (target == null || !target.isAlive()) return;

        if (this.damage < 0) {
            // ★ 回復処理（ダメージがマイナスの場合）
            target.heal(-this.damage);
            System.out.println("[SKILL-APPLY] Healed " + target.getName().getString() + " for " + (-this.damage));
        } else if (this.damage > 0) {
            // ★ ダメージ処理
            boolean hurt = target.hurt(type.create(attacker), this.damage);

            // ★ ノックバック（ダメージが通った場合のみ実行）
            if (hurt) {
                double dx = attacker.getX() - target.getX();
                double dz = attacker.getZ() - target.getZ();
                target.knockback(0.5D, dx, dz);
                System.out.println("[SKILL-APPLY] Damaged & Knockbacked " + target.getName().getString());
            }
        }

        // ★ ステータス効果（毒など）の適用
        for (StatusEffectSpec effect : effects) {
            effect.apply(target);
        }
    }
}