package com.mimic.monstermod.skill;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public record PotionEffectSpec(
        MobEffect effect,
        int duration,
        int amplifier
) {
    // 指定対象にポーション効果をパッと付与する
    public void apply(LivingEntity target) {
        if (target == null) return;
        target.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }
}