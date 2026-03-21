package com.mimic.monstermod.skill;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public record StatusEffectSpec(
        MobEffect effect,
        int duration,
        int amplifier
) {
    public void apply(LivingEntity target) {
        if (target == null) return;
        target.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }
}