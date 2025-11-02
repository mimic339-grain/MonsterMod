package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class BaseMonsterRenderer<T extends BaseMonsterEntity> extends EntityRenderer<T> {

    private static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");

    /** Renderer が保持するモデル */
    private final ModelPart modelRoot;

    /** Bone の Proxy マップ */
    private final Map<String, AnimationPlayerTemplate.ModelPartProxy> boneMap = new HashMap<>();

    public BaseMonsterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;

        // JSONモデルまたは LayerDefinition から生成
        this.modelRoot = createModel();
        autoInitBoneMap(modelRoot);
    }

    /** JSONモデルや LayerDefinition からモデル生成 */
    protected ModelPart createModel() {
        try {
            // Blockbench 等で出力した JSON に対応する LayerDefinition をロード
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // 仮サンプル: Blockbench JSON で出力する場合はここを差し替え
            root.addOrReplaceChild("body",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -2, 8, 8, 4),
                    PartPose.ZERO
            );
            root.addOrReplaceChild("head",
                    CubeListBuilder.create().texOffs(24, 0).addBox(-4, -8, -4, 8, 8, 8),
                    PartPose.ZERO
            );

            LayerDefinition layer = LayerDefinition.create(mesh, 64, 64);
            return layer.bakeRoot();

        } catch (Exception e) {
            MonsterMod.LOGGER.error("[MonsterMod] Model creation failed", e);
            return new ModelPart(64, 64, 0, 0);
        }
    }

    /** BoneMap 自動初期化 */
    private void autoInitBoneMap(ModelPart root) {
        boneMap.clear();
        registerPartsRecursive(root, "root");
        MonsterMod.LOGGER.info("[MonsterMod] Renderer boneMap initialized: " + boneMap.keySet());
    }

    private void registerPartsRecursive(ModelPart part, String name) {
        boneMap.put(name, new AnimationPlayerTemplate.ModelPartProxy() {
            @Override
            public void setRotation(Vector3f rot) {
                part.xRot = rot.x;
                part.yRot = rot.y;
                part.zRot = rot.z;
            }

            @Override
            public void setPosition(Vector3f pos) {
                part.x = pos.x;
                part.y = pos.y;
                part.z = pos.z;
            }

            @Override
            public void setScale(Vector3f scale) {
                // optional
            }
        });

        try {
            var field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ModelPart> children = (Map<String, ModelPart>) field.get(part);
            for (var entry : children.entrySet()) {
                registerPartsRecursive(entry.getValue(), entry.getKey());
            }
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[MonsterMod] Bone registration failed", e);
        }
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        BaseMonsterIdentity identity = entity.getIdentity();
        if (identity == null) return;

        poseStack.pushPose();

        // 座標・回転同期
        poseStack.translate(0.0, 0.0, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));

        // IdentityPose を Renderer 側で反映
        Map<String, Map<String, Vector3f>> interpolatedPose =
                AnimationPlayerTemplate.blend(
                        identity.lastBoneTransforms,
                        identity.animationPlayer != null ? identity.animationPlayer.getCurrentPose() : new HashMap<>(),
                        partialTicks
                );

        for (var entry : boneMap.entrySet()) {
            var transforms = interpolatedPose.get(entry.getKey());
            if (transforms != null) AnimationPlayerTemplate.applyPoseToProxy(entry.getValue(), transforms);
        }

        // 描画
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
        modelRoot.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        poseStack.popPose();
    }

    @Override
    public @Nullable ResourceLocation getTextureLocation(T entity) {
        BaseMonsterIdentity identity = entity.getIdentity();
        if (identity != null) {
            ResourceLocation tex = identity.getTexture();
            if (tex != null) return tex;
            MonsterMod.LOGGER.warn("Identity texture is null for entity: " + entity.getName().getString());
        } else {
            MonsterMod.LOGGER.warn("No identity assigned to entity: " + entity.getName().getString());
        }
        return DEFAULT_TEXTURE;
    }

}
