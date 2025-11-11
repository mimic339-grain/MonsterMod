package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * BaseMonsterRenderer — 完全版 YSMMOD式
 *
 * - Identity / BoneMap / ModelRoot の安全初期化
 * - renderInterpolated で正しい Pose を描画
 * - 描画されない原因の追跡にデバッグログ
 */
public class BaseMonsterRenderer<T extends BaseMonsterEntity> extends EntityRenderer<T> {

    private static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");

    public BaseMonsterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        MonsterMod.LOGGER.debug("[BaseMonsterRenderer] render called for {}", entity);

        BaseMonsterIdentity identity = entity.getIdentity();
        if (identity == null) {
            MonsterMod.LOGGER.warn("[BaseMonsterRenderer] identity == null for {}", entity);
            return;
        }

        // モデル初期化
        entity.ensureModelInitialized();
        if (entity.getModelRoot() == null) {
            MonsterMod.LOGGER.warn("[BaseMonsterRenderer] modelRoot is null for {}", entity);
            return;
        }

        // BoneMap 初期化
        if (identity.boneMap == null || identity.boneMap.isEmpty()) {
            MonsterMod.LOGGER.debug("[BaseMonsterRenderer] BoneMap empty, calling autoInitBoneMap for {}", entity);
            identity.autoInitBoneMap(entity);
        }

        // 描画
        poseStack.pushPose();
        try {
            identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[BaseMonsterRenderer] Exception during renderInterpolated for {}: {}", entity, e.toString());
        } finally {
            poseStack.popPose();
        }

        MonsterMod.LOGGER.debug("[BaseMonsterRenderer] render finished for {}", entity);
    }

    @Override
    public @Nullable ResourceLocation getTextureLocation(T entity) {
        BaseMonsterIdentity identity = entity.getIdentity();
        if (identity != null && identity.getTexture() != null) {
            return identity.getTexture();
        }
        return DEFAULT_TEXTURE;
    }
}
