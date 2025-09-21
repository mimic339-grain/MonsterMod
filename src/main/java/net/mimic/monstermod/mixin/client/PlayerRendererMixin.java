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
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            IPlayerIdentity identity = trans.getTransformedIdentity();
            MimicEntity.MimicAnimationState animState = trans.getAnimationState(trans.getTransformedMobId());

            System.out.println("[PlayerRendererMixin] render player=" + player.getName().getString() + " animState=" + animState
                    + " identity=" + (identity != null));

            if (identity != null) {
                identity.applyAnimationAndRender(player, entityYaw, partialTicks, poseStack, buffer, packedLight, animState);
                ci.cancel();
                return;
            }

            ClientMimicEntity cached = ClientMimicEntity.getOrCreate(player.getUUID());

            cached.tickUpdate(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), animState);

            cached.tick();
            clientRenderer.render(cached, entityYaw, partialTicks, poseStack, buffer, packedLight);
            clientRenderer.render(cached, entityYaw, partialTicks, poseStack, buffer, packedLight);
            ci.cancel();
        });
    }
}