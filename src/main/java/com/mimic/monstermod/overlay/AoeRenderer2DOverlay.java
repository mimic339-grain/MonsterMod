package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AoeMeshBuilder2D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AoeRenderer2DOverlay {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final float ALPHA = 0.35f;
    private static final double Y_OFFSET = 0.02;

    private static final ResourceLocation TEX =
            new ResourceLocation("monstermod", "textures/misc/attackpreview.png");

    private AoeRenderer2DOverlay(){}

    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            AoeMeshBuilder2D builder
    ){

        Level level = mc.level;
        if (level == null) return;

        VertexConsumer vc =
                buffers.getBuffer(RenderType.entityTranslucent(TEX));

        PoseStack.Pose pose = poseStack.last();

        List<AoeMeshBuilder2D.Quad> quads = builder.build();

        for (AoeMeshBuilder2D.Quad q : quads){

            Vec3[] p = q.pos();

            draw(
                    vc, pose,
                    resolve(level, p[0]),
                    resolve(level, p[1]),
                    resolve(level, p[2]),
                    resolve(level, p[3])
            );
        }
    }

    /* =========================
     * 各頂点ごとに地形追従
     * ========================= */
    private static Vec3 resolve(Level level, Vec3 p){

        double y = resolveSurfaceY(level, p);

        return new Vec3(
                p.x,
                y + Y_OFFSET,
                p.z
        );
    }

    private static double resolveSurfaceY(Level level, Vec3 pos){

        BlockPos base = BlockPos.containing(pos);

        for (int dy = 1; dy >= -1; dy--) {

            BlockPos bp = base.above(dy);
            BlockState state = level.getBlockState(bp);

            if (state.isAir()) continue;

            var shape = state.getCollisionShape(level, bp);
            if (shape.isEmpty()) continue;

            double top = shape.max(Direction.Axis.Y);
            return bp.getY() + top;
        }

        return pos.y;
    }

    /* =========================
     * 描画
     * ========================= */
    private static void draw(
            VertexConsumer vc,
            PoseStack.Pose pose,
            Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4
    ){

        vc.vertex(pose.pose(), (float)p1.x, (float)p1.y, (float)p1.z)
                .color(1f,0f,0f,ALPHA)
                .uv(0,0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();

        vc.vertex(pose.pose(), (float)p2.x, (float)p2.y, (float)p2.z)
                .color(1f,0f,0f,ALPHA)
                .uv(1,0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();

        vc.vertex(pose.pose(), (float)p3.x, (float)p3.y, (float)p3.z)
                .color(1f,0f,0f,ALPHA)
                .uv(1,1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();

        vc.vertex(pose.pose(), (float)p4.x, (float)p4.y, (float)p4.z)
                .color(1f,0f,0f,ALPHA)
                .uv(0,1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(pose.normal(),0,1,0)
                .endVertex();
    }
}