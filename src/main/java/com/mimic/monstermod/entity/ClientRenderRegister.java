package com.mimic.monstermod.entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.client.renderer.MimicRenderer;
@Mod.EventBusSubscriber(
        modid = MonsterMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class ClientRenderRegister {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntitieType.MIMIC.get(), MimicRenderer::new);
        MonsterMod.LOGGER.info("[ClientSetup] MimicRenderer registered");
    }
}