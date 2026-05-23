package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.render.OnibiRenderer;
import com.mimic.monstermod.entity.render.TornadoRenderer;
import com.mimic.monstermod.geo.renderer.HunterRenderer;
import com.mimic.monstermod.geo.renderer.MimicRenderer;
import com.mimic.monstermod.geo.renderer.YatagarasuRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityRendererRegister {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntitieType.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(ModEntitieType.YATAGARASU.get(), YatagarasuRenderer::new); // これでエラーが消えるはず
        event.registerEntityRenderer(ModEntitieType.HUNTER.get(), HunterRenderer::new);
        event.registerEntityRenderer(ModEntitieType.ONIBI.get(), OnibiRenderer::new);
        event.registerEntityRenderer(ModEntitieType.SPIRALONIBI.get(), OnibiRenderer::new);
        event.registerEntityRenderer(ModEntitieType.TORNADO.get(), TornadoRenderer::new);
        // 今後、他のモンスター（PRO_HERO等）が増えたらここに追加していけばOK！
    }
}