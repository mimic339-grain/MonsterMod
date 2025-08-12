package com.mimic.monster.mixin;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//AbstractClientPlayerとPlayerModelを対象に
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private LivingEntity cachedEntity = null;

    // 既存コンストラクタはprivateでOK
    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    // ①コンストラクタにInjectして独自のレイヤーを追加する例
    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Z)V", at = @At("RETURN"))
    private void onConstructor(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = this.getModel();
        // 例）レイヤーを追加（必要があれば）
        // this.addLayer(new YourCustomLayer(this, model, context));
    }

    // ②renderメソッドにInjectして変身描画を上書き
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)

    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack,
                          MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            if (!cap.isTransformed()) return;
            EntityType<?> type = cap.getTransformedType();
            if (type == null) return;

            if (cachedEntity == null || cachedEntity.getType() != type) {
                Entity entity = type.create(player.level());
                if (!(entity instanceof LivingEntity living)) return;
                cachedEntity = living;
            }

            cachedEntity.setYRot(player.getYRot());
            cachedEntity.setYBodyRot(player.yBodyRot);
            cachedEntity.setYHeadRot(player.yHeadRot);

            cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
            cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());

            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(cachedEntity)
                    .render(cachedEntity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

            ci.cancel();
        });
    }
}
