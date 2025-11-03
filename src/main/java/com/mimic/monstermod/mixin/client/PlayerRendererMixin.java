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
 * PlayerRenderer Mixin 完全版
 * - BaseMonsterIdentity に基づく変身描画
 * - ClientEntity生成 / ModelRoot初期化 / アニメ補間対応
 * - 元Player描画はキャンセル
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

                    // 未変身なら通常描画
                    if (!transformation.isTransformed()) return;

                    BaseMonsterIdentity identity = transformation.getIdentity();
                    if (identity == null) return;

                    // Client側 Entity を取得 or 生成
                    BaseMonsterEntity entity = identity.getEntity();
                    if (entity == null) {
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] Generating client entity for transformed player...");
                        identity.ensureClientEntity(player);
                        entity = identity.getEntity();
                        if (entity == null) {
                            MonsterMod.LOGGER.error("[PlayerRendererMixin] Failed to generate client entity!");
                            return;
                        }
                    }

                    // ModelRoot 初期化
                    if (entity.getModelRoot() == null) {
                        MonsterMod.LOGGER.debug("[PlayerRendererMixin] Initializing ModelRoot...");
                        entity.ensureModelInitialized();
                        if (entity.getModelRoot() == null) {
                            MonsterMod.LOGGER.error("[PlayerRendererMixin] ModelRoot still null after ensureModelInitialized!");
                            return;
                        }
                    }

                    poseStack.pushPose();

                    // プレイヤ/ー回転に合わせて向きを調整
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - player.getYHeadRot()));

                    MonsterMod.LOGGER.trace("[PlayerRendererMixin] Rendering transformed player as {} | Texture={}",
                            entity.getType().toShortString(),
                            identity.getTexture() != null ? identity.getTexture() : "DEFAULT");

                    try {
                        // Identity 側でアニメーション補間描画
                        identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);
                    } catch (Exception e) {
                        MonsterMod.LOGGER.error("[PlayerRendererMixin] renderInterpolated failed", e);
                    }

                    poseStack.popPose();

                    // 元 Player 描画をキャンセル
                    ci.cancel();
                });
    }

}
