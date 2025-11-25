package com.mimic.monstermod.client;

import com.mimic.monstermod.client.preview.AoeMarkerManager;
import com.mimic.monstermod.client.preview.AoeRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AoeMarkerManager.updateMarkers();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            List<AoeMarkerManager.AoeMarker> markers = AoeMarkerManager.getMarkers();
            if (!markers.isEmpty()) {
                AoeRenderer.renderAoeMarkers(
                        event.getPoseStack(),
                        event.getCamera(),
                        event.getPartialTick(),
                        markers
                );
            }
        }
    }
}
