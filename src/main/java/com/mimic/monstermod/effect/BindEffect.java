package com.mimic.monstermod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class BindEffect extends MobEffect {
    public BindEffect() {
        // カテゴリは有害(HARMFUL)、色は鎖をイメージした灰色
        super(MobEffectCategory.HARMFUL, 0x666666);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            // BIND専用パケットを送信
            com.mimic.monstermod.network.ModMessages.sendToPlayer(
                    new com.mimic.monstermod.network.server.S2C_SetEntityBindPacket(true), sp);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            // BINDを解除
            com.mimic.monstermod.network.ModMessages.sendToPlayer(
                    new com.mimic.monstermod.network.server.S2C_SetEntityBindPacket(false), sp);
        }
    }
}