package com.mimic.monster.entity;

import com.mimic.monster.entity.monster.Mimic;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

public class ModEvents {
    @EventBusSubscriber(
            modid = "monstermod",
            bus = Bus.MOD
    )
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
            event.put(ModEntityType.MIMIC.get(), Mimic.createAttributes().build());
        }
    }
}