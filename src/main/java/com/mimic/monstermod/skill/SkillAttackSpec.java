package com.mimic.monstermod.skill;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class SkillAttackSpec {

    public final float damage;
    public final DamageType type;
    public final List<StatusEffectSpec> effects;
    // ダミー作成用の引数なしコンストラクタ
    SkillAttackSpec() {
        this.damage = 0.0f;
        this.type = null; // または適切なデフォルト
        this.effects = java.util.Collections.emptyList();
    }
    public SkillAttackSpec(
            float damage,
            DamageType type,
            List<StatusEffectSpec> effects
    ) {
        this.damage = damage;
        this.type = type;
        this.effects = effects;
    }

    /**
     * 実ダメージ適用
     * @param attacker ダメージ発生源
     * @param target   被弾者
     */
    public void apply(LivingEntity attacker, LivingEntity target) {

        target.hurt(
                type.create(attacker),
                damage
        );

        for (StatusEffectSpec effect : effects) {
            effect.apply(target);
        }
    }
}
