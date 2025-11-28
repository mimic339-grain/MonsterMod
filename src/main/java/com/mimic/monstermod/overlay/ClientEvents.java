package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AttackPreview3DMath;
import com.mimic.monstermod.overlay.AoeMarkerManager.AoeMarker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
public class ClientEvents {

    // ============================================================
    //   UPDATE
    // ============================================================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AoeMarkerManager.updateMarkers();
        }
    }

    // ============================================================
    //   RENDER
    // ============================================================
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {

        // 1.20系 Forge は AFTER_PARTICLES が透明描画に最適
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.cameraEntity == null) return;

        List<AoeMarker> markers = AoeMarkerManager.getMarkers();
        if (markers.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        // forge / 1.20.1 ならこれが正しい取得方法
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // カメラシフト
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        // ============================================================
        //   3D マーカー
        // ============================================================
        List<AoeMarker> markers3D = markers.stream()
                .filter(m -> switch (m.shape) {
                    case BOX, SPHERE, CYLINDER, CAPSULE, FAN3D, CROSS3D, TRIANGLE_PRISM -> true;
                    default -> false;
                })
                .collect(Collectors.toList());

        for (AoeMarker m : markers3D) {
            AttackPreview3DMath.ShapeData data = m.get3DShapeData();
            if (data != null) {
                AoeRenderer3D.render(poseStack, bufferSource, data, event.getPartialTick());
            }
        }

        // ============================================================
        //   BLOCK2D マーカー
        // ============================================================
        List<AoeMarker> markersBlock2D = markers.stream()
                .filter(m -> switch (m.shape) {
                    case CIRCLE_BLOCK2D, FAN_BLOCK2D, TRIANGLE_BLOCK2D,
                         RECT_BLOCK2D, CROSS_BLOCK2D, RANDOM_BLOCK2D -> true;
                    default -> false;
                })
                .collect(Collectors.toList());

        if (!markersBlock2D.isEmpty()) {
            AoeRenderer2DBlock.renderAoeMarkers(poseStack, markersBlock2D);
        }

        // ============================================================
        //   2D（上からの描画）
        // ============================================================
        List<AoeMarker> markers2D = markers.stream()
                .filter(m -> switch (m.shape) {
                    case CIRCLE_2D, FAN_2D, TRIANGLE_2D,
                         RECT_2D, CROSS_2D, RANDOM_2D -> true;
                    default -> false;
                })
                .collect(Collectors.toList());

        if (!markers2D.isEmpty()) {
            AoeRenderer2D.renderAoeMarkers(poseStack, markers2D);
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }
}
