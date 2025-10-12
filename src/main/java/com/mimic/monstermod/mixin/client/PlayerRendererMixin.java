package com.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private PlayerRendererMixin() {
        super(null, null, 0f);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            // mobId から BaseMonsterIdentity を取得
            var mobId = transformation.getTransformedMobId();
            if (mobId == null) return;

            var identity = BaseMonsterIdentityRegistry.getIdentity(mobId);
            if (identity == null) return;

            // 描画共通処理を呼ぶ
            identity.applyAnimationAndRender(player, entityYaw, partialTicks, poseStack, buffer, packedLight);

            // 元のプレイヤー描画をスキップ
            ci.cancel();
        });
    }
}
