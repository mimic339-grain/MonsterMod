package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.geo.renderer.HunterRenderer;
import com.mimic.monstermod.geo.renderer.MimicRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityRendererRegister {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // ミミックの登録
        event.registerEntityRenderer(ModEntitieType.MIMIC.get(), MimicRenderer::new);

        // ハンターの登録
        event.registerEntityRenderer(ModEntitieType.HUNTER.get(), HunterRenderer::new);

        // 今後、他のモンスター（PRO_HERO等）が増えたらここに追加していけばOK！
    }
}