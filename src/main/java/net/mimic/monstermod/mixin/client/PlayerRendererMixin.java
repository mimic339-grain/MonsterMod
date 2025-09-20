package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.client.renderer.ClientMimicRenderer;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final ClientMimicRenderer clientRenderer;

    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
        this.clientRenderer = new ClientMimicRenderer(context);
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

            // Identity が存在する場合は Identity 側で描画
            IPlayerIdentity identity = transformation.getTransformedIdentity();
            MimicEntity.MimicAnimationState animState = transformation.getAnimationState(transformation.getTransformedMobId());

            if (identity != null) {
                identity.applyAnimationAndRender(player, entityYaw, partialTicks, poseStack, buffer, packedLight, animState);
                ci.cancel();
                return;
            }

            // ClientMimicEntity 描画
            ClientMimicEntity clientEntity = ClientMimicEntity.getOrCreate(player.getUUID());

            // 位置と回転だけを毎フレーム更新
            clientEntity.setPosAndRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            // ★ animState の setAnimation はここでは行わない
            //    → 状態変化に応じて ClientMimicEntity が自動で管理する

            // GeckoLib の tick 進行
            clientEntity.tick();

            // 描画
            clientRenderer.render(clientEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            ci.cancel();
        });
    }
}
