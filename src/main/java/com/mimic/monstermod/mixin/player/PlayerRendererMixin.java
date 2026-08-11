package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.weapon.WeaponItemInHandLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        // 1. モンスター変身のチェック
        var monsterXform = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).orElse(null);
        if (monsterXform != null && monsterXform.isTransformed()) {
            BaseIdentity identity = monsterXform.getIdentity();
            if (identity != null) {
                renderAndCancel(identity, player, partialTicks, poseStack, buffer, packedLight, ci);
                return;
            }
        }

        // 2. ハンター抜刀状態のチェック（モンスター変身していない時のみ）
        var hunterXform = player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).orElse(null);
        if (hunterXform != null && hunterXform.isActive() && !hunterXform.isSheathed()) {
            // ★ HunterTransformation が持っている Identity を直接使う
            BaseIdentity hunterId = hunterXform.getIdentity();

            if (hunterId != null) {
                // スキン情報を同期
                if (hunterId.getEntity() instanceof com.mimic.monstermod.entity.HunterEntity hunterEnt) {
                    hunterEnt.setPlayerInfo(player.getUUID(), player.getModelName().equals("slim"));
                }
                renderAndCancel(hunterId, player, partialTicks, poseStack, buffer, packedLight, ci);
            }
        }
    }
    // 共通描画メソッドでコードをスッキリさせる
    private void renderAndCancel(BaseIdentity identity, AbstractClientPlayer player, float partialTicks,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        identity.copyFromPlayerClient(player);
        identity.render(player, partialTicks, poseStack, buffer, packedLight);
        // 変身中プレイヤーの部位当たり判定を表示(実体のモンスターと同じ設定・見え方)
        com.mimic.monstermod.util.HitboxRenderUtil.renderTransformedPlayerHitboxes(player, poseStack, buffer);
        ci.cancel();
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void monstermod$addWeaponLayer(CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer)(Object)this;

        ItemInHandRenderer itemRenderer =
                Minecraft.getInstance().gameRenderer.itemInHandRenderer;

        // ★ vanilla は残したまま
        renderer.addLayer(
                new WeaponItemInHandLayer<>(renderer, itemRenderer)
        );
    }
}