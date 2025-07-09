package net.mimic.monstermod.client.renderer;

import net.mimic.monstermod.client.model.MimicModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.custom.MimicEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MimicRenderer extends GeoEntityRenderer<MimicEntity> {
    public MimicRenderer(EntityRendererProvider.Context renderProvider) {
        super(renderProvider, new MimicModel());
        this.shadowRadius = 0.4f; // 影のサイズを調整
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity instance) {
        // ここが重要：正しいテクスチャのパスを指定
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic/mimic.png");
    }
}