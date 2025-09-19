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

        // --- 補間位置のみ反映 ---
        double interpX = entity.getInterpolatedX(partialTicks);
        double interpY = entity.getInterpolatedY(partialTicks);
        double interpZ = entity.getInterpolatedZ(partialTicks);

        poseStack.translate(
                interpX - entity.getX(),
                interpY - entity.getY(),
                interpZ - entity.getZ()
        );
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
