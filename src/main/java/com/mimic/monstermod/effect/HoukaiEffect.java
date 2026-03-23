package com.mimic.monstermod.effect;

import com.mimic.monstermod.util.DelayUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class HoukaiEffect extends MobEffect {
    public HoukaiEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B0082); // 濃い紫
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map, int amplifier) {
        super.addAttributeModifiers(entity, map, amplifier);

        // 100 ticks (5秒) 後に発動する予約
        // ※ポーションの残り時間を動的に取る場合は、別途工夫が必要ですが、まずはこれで動きます。
        int duration = 100;

        DelayUtil.setDelay(entity, duration, (target) -> {
            float damage = (amplifier + 1) * 10.0f;
            target.hurt(target.damageSources().magic(), damage);
            // ここで爆発エフェクトなどを出すと最高に「崩壊」っぽくなります
        });
    }
}