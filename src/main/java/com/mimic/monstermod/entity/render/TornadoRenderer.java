package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.mixin.accessor.RenderStateShardAccessor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TornadoRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("modid", "textures/particle/tornado.png");

    private static final float RADIUS = 0.50f;
    private static final float LENGTH = 20.0f;
    private static final int SEGMENTS = 20;
    private static final float ROTATION_SPEED = 0.50f;
    private static final float INNER_GLOW = 1.5f;
    private static final float PULSE_SPEED = 2.0f;
    private static final float SPIRAL_TWIST = 0.0f;

    public TornadoRenderer(EntityRendererProvider.Context context) { super(context); }

    private static RenderType createTornadoRenderType() {
        return RenderType.create("tornado_render",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShardAccessor.getEntityTranslucentShader())
                        .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, false, false))
                        .setTransparencyState(RenderStateShardAccessor.getTranslucentTransparency())
                        .setCullState(RenderStateShardAccessor.getNoCull())
                        .setLightmapState(RenderStateShardAccessor.getLightmap())
                        .createCompositeState(true));
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float time = (entity.tickCount + partialTick) * ROTATION_SPEED;
        VertexConsumer vc = buffer.getBuffer(createTornadoRenderType());
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int lengthSegments = 40;
        float pulse = 1.0f + (float)Math.sin(time * PULSE_SPEED) * 0.2f;

        for (int i = 0; i < lengthSegments; i++) {
            float p1 = (float) i / lengthSegments;
            float p2 = (float) (i + 1) / lengthSegments;
            float y1 = p1 * LENGTH;
            float y2 = p2 * LENGTH;
            float r = RADIUS * pulse;

            for (int j = 0; j < SEGMENTS; j++) {
                float u1 = (float) j / SEGMENTS;
                float u2 = (float) (j + 1) / SEGMENTS;
                float a1 = u1 * (float)Math.PI * 2.0f + time + p1 * SPIRAL_TWIST;
                float a2 = u2 * (float)Math.PI * 2.0f + time + p1 * SPIRAL_TWIST;

                vc.vertex(pose, (float)Math.cos(a1)*r, y1, (float)Math.sin(a1)*r)
                        .color(255, 200, 0, 220)
                        .uv(u1, p1).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880).normal(normal, 0, 1, 0).endVertex();
                vc.vertex(pose, (float)Math.cos(a2)*r, y1, (float)Math.sin(a2)*r)
                        .color(255, 200, 0, 220)
                        .uv(u2, p1).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880).normal(normal, 0, 1, 0).endVertex();
                vc.vertex(pose, (float)Math.cos(a2)*r, y2, (float)Math.sin(a2)*r)
                        .color(255, 200, 0, 220)
                        .uv(u2, p2).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880).normal(normal, 0, 1, 0).endVertex();
                vc.vertex(pose, (float)Math.cos(a1)*r, y2, (float)Math.sin(a1)*r)
                        .color(255, 200, 0, 220)
                        .uv(u1, p2).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(15728880).normal(normal, 0, 1, 0).endVertex();
            }
        }
    }

    @Override
    public boolean shouldRender(T entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double x, double y, double z) { return true; }
    @Override
    public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
}
/*
    private static RenderType createTornadoRenderType() {
        return RenderType.create("tornado_render",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShardAccessor.getEntityTranslucentShader())
                        .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, false, false))
                        .setTransparencyState(RenderStateShardAccessor.getTranslucentTransparency())
                        .setCullState(RenderStateShardAccessor.getNoCull())
                        .setLightmapState(RenderStateShardAccessor.getLightmap())
                        .createCompositeState(true));
    }
*/