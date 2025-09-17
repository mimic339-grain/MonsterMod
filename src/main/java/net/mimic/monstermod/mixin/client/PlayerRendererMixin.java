package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.client.renderer.ClientMimicRenderer;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.PlayerEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final ClientMimicRenderer clientRenderer;

    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
        this.clientRenderer = new ClientMimicRenderer(context);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                          PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            if (!cap.isTransformed()) return;

            // Identity 描画優先
            IPlayerIdentity identity = cap.getTransformedIdentity();
            if (identity != null) {
                identity.applyAnimationAndRender(player, entityYaw, partialTicks, poseStack, buffer, packedLight,
                        cap.getMonsterState(cap.getTransformedMobId()));
                ci.cancel();
                return;
            }

            // ClientMimicEntity を取得
            UUID playerId = player.getUUID();
            ClientMimicEntity clientEntity = PlayerEntityCache.getOrCreateClient(playerId, ClientMimicEntity::new);

            clientEntity.setPosAndRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            if (cap.getMonsterState(cap.getTransformedMobId()) != null) {
                clientEntity.setAnimationState(
                        cap.getMonsterState(cap.getTransformedMobId()).getAnimationEnum()
                );
            }

            clientRenderer.render(clientEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            ci.cancel();
        });
    }
}
