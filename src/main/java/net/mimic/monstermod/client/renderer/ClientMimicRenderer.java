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

    // ClientMimicEntity の座標・回転をそのまま描画に反映
    @Override
    public void render(ClientMimicEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        // 回転・座標を poseStack に反映
        poseStack.pushPose();
        poseStack.translate(entity.getX(), entity.getY(), entity.getZ());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.getXRot()));

        // GeckoLib の描画呼び出し
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
