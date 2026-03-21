package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AoeMeshBuilder3D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AoeRenderer3D {

    private static final float ALPHA = 0.3f;//透明度
    private static final float OFFSET = 0.01f;//浮かせる度合い

    private AoeRenderer3D(){}

    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            AoeMeshBuilder3D builder,
            ResourceLocation texture
    ){

        VertexConsumer vc =
                buffers.getBuffer(RenderType.entityTranslucent(texture));

        PoseStack.Pose pose = poseStack.last();

        List<AoeMeshBuilder3D.Quad> quads = builder.build();

        for (AoeMeshBuilder3D.Quad q : quads){

            Vec3[] p = q.pos();

            // 表面
            draw(vc, pose, p[0], p[1], p[2], p[3]);

            // 法線計算
            Vec3 normal = p[1].subtract(p[0])
                    .cross(p[2].subtract(p[0]))
                    .normalize()
                    .scale(OFFSET);

            // 裏面　これがないと中からみえなかったり外から見えない
            drawOffset(vc, pose,
                    p[3].add(normal),
                    p[2].add(normal),
                    p[1].add(normal),
                    p[0].add(normal)
            );
        }
    }

    private static void draw(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4
    ){
        put(vc, pose, p1, 0,0);
        put(vc, pose, p2, 1,0);
        put(vc, pose, p3, 1,1);
        put(vc, pose, p4, 0,1);
    }

    private static void drawOffset(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4
    ){
        put(vc, pose, p1, 0,0);
        put(vc, pose, p2, 1,0);
        put(vc, pose, p3, 1,1);
        put(vc, pose, p4, 0,1);
    }

    private static void put(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p,
            float u, float v
    ){
        vc.vertex(pose.pose(), (float)p.x, (float)p.y, (float)p.z)
                .color(1f,0f,0f,ALPHA)
                .uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();
    }
}