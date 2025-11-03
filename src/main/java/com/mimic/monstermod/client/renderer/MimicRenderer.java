package com.mimic.monstermod.client.renderer;

import com.mimic.monstermod.client.BaseMonsterRenderer;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MimicRenderer<T extends BaseMonsterEntity> extends BaseMonsterRenderer<T> {

    private static final ResourceLocation MIMIC_TEXTURE =
            new ResourceLocation("monstermod", "textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.7f;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MIMIC_TEXTURE;
    }
}
