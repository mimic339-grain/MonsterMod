package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * プレイヤーの手の武器描画を
 * Hunter状態・納刀状態に応じて制御する Mixin
 * Forge 1.20.1-47.4.1 対応版
 */
@Mixin(ItemInHandLayer.class)
public class HeldItemLayerHunterMixin {
    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void controlHunterWeaponRender(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LocalPlayer player)) return;

        HunterTransformation hunter = player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .orElse(null);
        if (hunter == null) return;

        if (hunter.isActive() && !hunter.isSheathed()) {
            ci.cancel();
        }
    }
}