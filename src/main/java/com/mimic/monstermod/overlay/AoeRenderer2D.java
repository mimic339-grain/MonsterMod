package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AoeMeshBuilder2D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AoeRenderer2D {

    private static final float ALPHA = 0.3f;

    private AoeRenderer2D(){}

    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            AoeMeshBuilder2D builder,
            ResourceLocation texture
    ){

        VertexConsumer vc =
                buffers.getBuffer(RenderType.entityTranslucent(texture));

        PoseStack.Pose pose = poseStack.last();

        List<AoeMeshBuilder2D.Quad> quads = builder.build();

        for (AoeMeshBuilder2D.Quad q : quads){

            Vec3[] p = q.pos();

            // ★ 順番変更（これ超重要）
            draw(vc, pose, p[0], p[3], p[2], p[1]);
        }
    }

    private static void draw(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4
    ){

        float yOffset = 0.01f;

        vc.vertex(pose.pose(), (float)p1.x, (float)p1.y + yOffset, (float)p1.z)
                .color(1f,0f,0f,ALPHA)
                .uv(0,0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();

        vc.vertex(pose.pose(), (float)p2.x, (float)p2.y + yOffset, (float)p2.z)
                .color(1f,0f,0f,ALPHA)
                .uv(1,0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();

        vc.vertex(pose.pose(), (float)p3.x, (float)p3.y + yOffset, (float)p3.z)
                .color(1f,0f,0f,ALPHA)
                .uv(1,1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();

        vc.vertex(pose.pose(), (float)p4.x, (float)p4.y + yOffset, (float)p4.z)
                .color(1f,0f,0f,ALPHA)
                .uv(0,1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();
    }
}