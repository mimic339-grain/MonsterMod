package com.mimic.monstermod.skill;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

public class SkillEffectSpec {
    public final float damage;
    public final DamageType damageType;
    public final SkillType skillType;
    public final List<PotionEffectSpec> effects;

    public SkillEffectSpec() {
        this(0.0f, DamageType.PHYSICAL, SkillType.STRIKE, Collections.emptyList());
    }

    public SkillEffectSpec(float damage, DamageType damageType, SkillType skillType, List<PotionEffectSpec> effects) {
        this.damage = damage;
        this.damageType = damageType;
        this.skillType = skillType;
        this.effects = effects;
    }
    public void onPreviewTick(LivingEntity attacker, int remainingTicks) {
        // デフォルトは何もしない。サブクラスでオーバーライドする。
    }
    /**
     * 通常の1回切りスキル用（互換性維持）
     */
    public void apply(LivingEntity attacker, @Nullable LivingEntity target) {
        this.apply(attacker, target, 0);
    }

    /**
     * ★ 継続スキル用（SkillUtilのループからこれを呼ぶようにする）
     */
    public void apply(LivingEntity attacker, @Nullable LivingEntity target, int tick) {
        // tick付きの処理を実行
        applyToCaster(attacker, tick);

        // 初回のみターゲットへのダメージやデバフを適用
        if (tick == 0 && target != null && target.isAlive()) {
            applyToTarget(attacker, target);
        }
    }

    /**
     * サブクラスで tick ごとの処理を書くための土台
     */
    protected void applyToCaster(LivingEntity attacker, int tick) {
        // デフォルトでは初回だけ引数なし版を呼ぶ
        if (tick == 0) {
            applyToCaster(attacker);
        }
    }

    protected void applyToCaster(LivingEntity attacker) {
        // 何もしない（サブクラスで単発処理を書く用）
    }

    private void applyToTarget(LivingEntity attacker, LivingEntity target) {
        if (this.damage < 0) {
            target.heal(-this.damage);
        } else if (this.damage > 0) {
            boolean hurt = target.hurt(damageType.create(attacker), this.damage);
            if (hurt) {
                double dx = attacker.getX() - target.getX();
                double dz = attacker.getZ() - target.getZ();
                target.knockback(0.5D, dx, dz);
            }
        }
        for (PotionEffectSpec effect : effects) {
            effect.apply(target);
        }
    }
}