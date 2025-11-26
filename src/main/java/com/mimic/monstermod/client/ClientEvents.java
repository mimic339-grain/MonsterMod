package com.mimic.monstermod.client;

import com.mimic.monstermod.client.preview.AoeMarkerManager;
import com.mimic.monstermod.client.preview.AoeMarkerManager.AoeMarker;
import com.mimic.monstermod.client.preview.AoeRenderer2D;
import com.mimic.monstermod.client.preview.AoeRenderer2DBlock;
import com.mimic.monstermod.client.preview.AoeRenderer3D;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.stream.Collectors;

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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        List<AoeMarker> markers = AoeMarkerManager.getMarkers();
        if (markers.isEmpty()) return;

        // 3Dマーカー
        List<AoeMarker> markers3D = markers.stream()
                .filter(m -> switch (m.shape) {
                    case BOX, SPHERE, CYLINDER, CAPSULE, FAN3D, CROSS3D, TRIANGLE_PRISM -> true;
                    default -> false;
                })
                .collect(Collectors.toList());
        if (!markers3D.isEmpty()) {
            AoeRenderer3D.renderAoeMarkers(event.getPoseStack(), markers3D);
        }

        // Block2Dマーカー
        List<AoeMarker> markersBlock2D = markers.stream()
                .filter(m -> switch (m.shape) {
                    case CIRCLE_BLOCK2D, FAN_BLOCK2D, TRIANGLE_BLOCK2D, RECT_BLOCK2D, CROSS_BLOCK2D, RANDOM_BLOCK2D -> true;
                    default -> false;
                })
                .collect(Collectors.toList());
        if (!markersBlock2D.isEmpty()) {
            AoeRenderer2DBlock.renderAoeMarkers(event.getPoseStack(), markersBlock2D);
        }

        // 2D描画マーカー
        List<AoeMarker> markers2D = markers.stream()
                .filter(m -> switch (m.shape) {
                    case CIRCLE_2D, FAN_2D, TRIANGLE_2D, RECT_2D, CROSS_2D, RANDOM_2D -> true;
                    default -> false;
                })
                .collect(Collectors.toList());
        if (!markers2D.isEmpty()) {
            AoeRenderer2D.renderAoeMarkers(event.getPoseStack(), markers2D);
        }
    }
}