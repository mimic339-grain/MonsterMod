package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
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
        // PlayerTransformation が存在していて変身中かチェック
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            // 変身 Identity を取得
            MimicIdentity identity = (MimicIdentity) transformation.getTransformedIdentity();
            if (identity == null) return;

            // ClientMimicEntity を取得
            ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(player.getUUID());

            // ----- 座標・回転・アニメーション更新・描画を一括 -----
            identity.applyAnimationAndRender(
                    player,
                    entityYaw,
                    partialTicks,
                    poseStack,
                    buffer,
                    packedLight
            );

            // 元のプレイヤー描画をスキップ
            ci.cancel();
        });
    }
}
