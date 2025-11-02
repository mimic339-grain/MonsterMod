package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class BaseMonsterRenderer<T extends BaseMonsterEntity> extends EntityRenderer<T> {

    private static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation("monstermod", "textures/entity/mimic.png");

    public BaseMonsterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        BaseMonsterIdentity identity = entity.getIdentity();
        if (identity == null || entity.getModelRoot() == null) return;

        poseStack.pushPose();

        // 座標・回転同期
        poseStack.translate(0.0, 0.0, 0.0); // 必要ならオフセット
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));

        // Identity Pose を反映
        identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    public @Nullable ResourceLocation getTextureLocation(T entity) {
        BaseMonsterIdentity identity = entity.getIdentity();

        if (identity != null) {
            ResourceLocation tex = identity.getTexture();
            if (tex != null) return tex;

            // Identity はあるがテクスチャが取得できない場合
            MonsterMod.LOGGER.warn("Identity texture is null for entity: " + entity.getName().getString());
            return DEFAULT_TEXTURE;
        }

        // Identity がない場合
        MonsterMod.LOGGER.warn("No identity assigned to entity: " + entity.getName().getString());
        return DEFAULT_TEXTURE;
    }
}
