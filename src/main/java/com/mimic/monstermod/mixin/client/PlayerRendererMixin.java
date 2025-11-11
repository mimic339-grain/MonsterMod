package com.mimic.monstermod.mixin.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * PlayerRenderer Mixin — 完全版 YSMMOD対応
 *
 * - Identity/Entity/BoneMap の安全初期化
 * - renderInterpolated による正しい描画
 * - 描画されない問題を回避
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderIdentity(AbstractClientPlayer player,
                                float entityYaw,
                                float partialTicks,
                                PoseStack poseStack,
                                MultiBufferSource buffer,
                                int packedLight,
                                CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {

                    if (!transformation.isTransformed()) return;

                    BaseMonsterIdentity identity = transformation.getIdentity();
                    if (identity == null) {
                        MonsterMod.LOGGER.warn("[PlayerRendererMixin] Identity is null for transformed player {}", player.getName().getString());
                        return;
                    }

                    BaseMonsterEntity entity = identity.getEntity();
                    if (entity == null) {
                        identity.ensureClientEntity(player);
                        entity = identity.getEntity();
                        if (entity == null) {
                            MonsterMod.LOGGER.warn("[PlayerRendererMixin] Failed to spawn client entity for player {}", player.getName().getString());
                            return;
                        }
                        MonsterMod.LOGGER.info("[PlayerRendererMixin] Client entity spawned: {}", entity.getUUID());
                    }

                    // --- 回転同期 ---
                    entity.setXRot(player.getXRot());
                    entity.setYRot(player.getYRot());
                    entity.yHeadRot = player.yHeadRot;
                    entity.yBodyRot = player.yBodyRot;

                    // --- モデル初期化 & BoneMap ---
                    entity.ensureModelInitialized();
                    if (identity.boneMap == null || identity.boneMap.isEmpty()) {
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] BoneMap empty, initializing for entity {}", entity);
                        identity.autoInitBoneMap(entity);
                    }

                    ModelPart root = entity.getModelRoot();
                    if (root == null) {
                        MonsterMod.LOGGER.warn("[PlayerRendererMixin] Model root is null for entity {}", entity);
                        return;
                    }

                    // --- BoneMap vs ModelPart nameチェック (任意デバッグ) ---
                    try {
                        Field childrenField = ModelPart.class.getDeclaredField("children");
                        childrenField.setAccessible(true);
                        Map<String, ModelPart> children = (Map<String, ModelPart>) childrenField.get(root);
                        for (String key : identity.boneMap.keySet()) {
                            if (!children.containsKey(key)) {
                                MonsterMod.LOGGER.warn("[PlayerRendererMixin] BoneMap key '{}' missing in ModelPart children", key);
                            }
                        }
                    } catch (Exception e) {
                        MonsterMod.LOGGER.error("[PlayerRendererMixin] Error checking BoneMap vs ModelPart", e);
                    }

                    // --- 描画 ---
                    poseStack.pushPose();
                    try {
                        identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);
                    } catch (Exception e) {
                        MonsterMod.LOGGER.error("[PlayerRendererMixin] Exception during renderInterpolated", e);
                    } finally {
                        poseStack.popPose();
                    }

                    // オリジナル PlayerRenderer 描画をスキップ
                    ci.cancel();
                });
    }
}
