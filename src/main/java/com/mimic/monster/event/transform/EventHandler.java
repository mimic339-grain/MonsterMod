package com.mimic.monster.event.transform;

import com.mimic.monster.capability.CapabilityRegistry;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "monstermod")
public class EventHandler {

    @SubscribeEvent
    //EntityにCapabilityをつけるため
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            //CAPABILITY名をつける
            event.addCapability(new ResourceLocation("monstermod", "transform"), new CapabilityRegistry.Provider());
        }
    }
}
