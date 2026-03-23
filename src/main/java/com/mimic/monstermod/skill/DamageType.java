package com.mimic.monstermod.skill;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public enum DamageType {
    PHYSICAL,
    MAGIC,
    POISON;

    public DamageSource create(LivingEntity attacker) {
        return switch (this) {
            case PHYSICAL -> attacker.damageSources().mobAttack(attacker);
            case MAGIC, POISON -> attacker.damageSources().magic();
        };
    }
}