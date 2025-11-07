package com.mimic.monstermod.mixin.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerRenderer Mixin 完全Forge対応版
 * - LazyOptional対応（ifPresentのみ）
 * - 安全なnullチェックとメソッド互換処理
 * - 詳細ログ付き
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

        MonsterMod.LOGGER.trace("[PlayerRendererMixin] >>> render() called for {}", player.getName().getString());

        // ForgeのLazyOptionalには ifPresentOrElse がないため、ifPresent を使用
        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {

                    // --- 変身中チェック ---
                    boolean transformed = false;
                    try {
                        // あなたのクラスに応じて変更（例: isActive(), hasIdentity() など）
                        transformed = transformation.isTransformed();
                    } catch (Throwable ignored) {
                        MonsterMod.LOGGER.warn("[PlayerRendererMixin] transformation.isTransformed() not found, skipping check");
                        transformed = true; // 安全のため true 扱い
                    }

                    if (!transformed) {
                        MonsterMod.LOGGER.trace("[PlayerRendererMixin] Player '{}' is not transformed -> skip custom render", player.getName().getString());
                        return;
                    }

                    // --- Identity取得 ---
                    BaseMonsterIdentity identity = null;
                    try {
                        identity = transformation.getIdentity();
                    } catch (Throwable ignored) {
                        MonsterMod.LOGGER.error("[PlayerRendererMixin] transformation.getIdentity() not found");
                    }
                    if (identity == null) return;

                    MonsterMod.LOGGER.debug("[PlayerRendererMixin] Rendering transformed player '{}' as identity '{}'",
                            player.getName().getString(), identity.getId());

                    // --- Entity 確保 ---
                    BaseMonsterEntity entity = identity.getEntity();
                    if (entity == null) {
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] identity.getEntity() returned null, attempting ensureClientEntity...");
                        identity.ensureClientEntity(player);
                        entity = identity.getEntity();
                        if (entity == null) {
                            MonsterMod.LOGGER.error("[PlayerRendererMixin] Failed to obtain client entity after ensureClientEntity()");
                            return;
                        }
                        MonsterMod.LOGGER.info("[PlayerRendererMixin] Client entity created successfully: {}", entity.getType().toShortString());
                    }

                    // --- ModelRoot 初期化 ---
                    if (entity.getModelRoot() == null) {
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] ModelRoot == null, calling ensureModelInitialized...");
                        entity.ensureModelInitialized();
                        if (entity.getModelRoot() == null) {
                            MonsterMod.LOGGER.error("[PlayerRendererMixin] ModelRoot still null after ensureModelInitialized()");
                            return;
                        }
                        MonsterMod.LOGGER.info("[PlayerRendererMixin] ModelRoot successfully initialized for {}", identity.getId());
                    }

                    // --- BoneMap 初期化確認 ---
                    boolean hasBoneMap = false;
                    try {
                        hasBoneMap = identity.boneMap != null && !identity.boneMap.isEmpty();
                    } catch (Throwable ignored) {
                        MonsterMod.LOGGER.warn("[PlayerRendererMixin] boneMap field not accessible in BaseMonsterIdentity");
                    }

                    if (!hasBoneMap) {
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] BoneMap empty -> autoInitBoneMap()");
                        identity.autoInitBoneMap(entity);
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] BoneMap initialized");
                    }

                    // --- 描画 ---
                    poseStack.pushPose();
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - player.getYHeadRot()));

                    try {
                        MonsterMod.LOGGER.trace("[PlayerRendererMixin] Calling renderInterpolated() for identity '{}'", identity.getId());
                        identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] renderInterpolated() completed successfully");
                    } catch (Exception e) {
                        MonsterMod.LOGGER.error("[PlayerRendererMixin] renderInterpolated() threw exception", e);
                    }

                    poseStack.popPose();

                    ci.cancel();
                    MonsterMod.LOGGER.trace("[PlayerRendererMixin] <<< Cancelled default player render for {}", player.getName().getString());
                });
    }
}
