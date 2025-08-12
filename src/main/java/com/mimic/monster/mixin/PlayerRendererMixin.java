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

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }
    //Head割り込み
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack,
                         MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        //ForgeのCapabilityを取得し、変身状態かどうかを確認
        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            if (!cap.isTransformed()) return;
            //Capabilityから変身先のEntityType を取得
            EntityType<?> type = cap.getTransformedType();
            if (type == null) return;
            //キャッシュが無い、または違う種類のエンティティなら新しく作る
            if (cachedEntity == null || cachedEntity.getType() != type) {
                Entity entity = type.create(player.level());
                if (!(entity instanceof LivingEntity living)) return;
                cachedEntity = living;
            }

            // 回転
            cachedEntity.setYRot(player.getYRot());
            cachedEntity.setYBodyRot(player.yBodyRot);
            cachedEntity.setYHeadRot(player.yHeadRot);

            // 歩行アニメーション
            cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
            cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());
            //変身後エンティティをレンダラーに渡して描画
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(cachedEntity)
                    .render(cachedEntity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            //元のプレイヤー描画をスキップ
            ci.cancel();
        });
    }
}
