package net.mimic.monstermod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.client.model.ClientMimicModel;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ClientMimicRenderer extends GeoEntityRenderer<ClientMimicEntity> {

    public ClientMimicRenderer(EntityRendererProvider.Context context) {
        super(context, new ClientMimicModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(ClientMimicEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // 描画用フィールドを使用して、Entity 本体の座標や回転に依存しない
        poseStack.translate(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entity.getRenderYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getRenderXRot()));

        // GeckoLib に描画を委譲
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
