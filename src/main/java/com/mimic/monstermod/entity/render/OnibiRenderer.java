package com.mimic.monstermod.entity.render;

import com.mimic.monstermod.entity.obj.OnibiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class OnibiRenderer extends EntityRenderer<OnibiEntity> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[10];
    static {
        for (int i = 0; i < 10; i++) {
            TEXTURES[i] = new ResourceLocation("monstermod", "textures/entity/onibi/onibi" + (i + 1) + ".png");
        }
    }

    public OnibiRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(OnibiEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {


        poseStack.pushPose();

        // 画像の描画位置調整
        poseStack.translate(0.0D, 0.4D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        int age = entity.tickCount;
        int frame = (age < 3) ? age : 3 + (age - 3) % 7;
        ResourceLocation texture = TEXTURES[frame];

        float s = 0.8F;
        Matrix4f mat = poseStack.last().pose();
        VertexConsumer v = buffer.getBuffer(RenderType.entityTranslucent(texture));

        v.vertex(mat, -s, -s, 0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(0).uv2(packedLight).normal(0, 1, 0).endVertex();
        v.vertex(mat, s, -s, 0).color(255, 255, 255, 255).uv(1, 1).overlayCoords(0).uv2(packedLight).normal(0, 1, 0).endVertex();
        v.vertex(mat, s, s, 0).color(255, 255, 255, 255).uv(1, 0).overlayCoords(0).uv2(packedLight).normal(0, 1, 0).endVertex();
        v.vertex(mat, -s, s, 0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(0).uv2(packedLight).normal(0, 1, 0).endVertex();

        poseStack.popPose();

    }

    @Override
    public ResourceLocation getTextureLocation(OnibiEntity e) { return TEXTURES[0]; }
}