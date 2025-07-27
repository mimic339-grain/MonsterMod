package net.mimic.monstermod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MimicRenderer extends GeoEntityRenderer<MimicEntity> {
    public MimicRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MimicModel());
        this.shadowRadius = 0.4f; // エンティティの影のサイズ
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity animatable) {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }

    @Override
    public void render(MimicEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 必要に応じて、Mimicの透明度や追加のレンダリング設定をここで行います。
        // super.renderの前に設定することで、モデル全体に適用されます。
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}