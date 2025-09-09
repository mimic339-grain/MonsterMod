package net.mimic.monstermod.mixin.client;

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
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.client.renderer.identity.PlayerIdentityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    // プレイヤーごとのキャッシュ保持（マルチプレイ対応）
    private static final Map<UUID, LivingEntity> playerEntityCache = new ConcurrentHashMap<>();

    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                          PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            if (!cap.isTransformed()) return;

            // GeckoLib Identity 描画優先
            IPlayerIdentity identity = cap.getTransformedIdentity();
            if (identity != null) {
                PlayerIdentityRenderer.render(identity, player, entityYaw, partialTicks, poseStack, buffer, packedLight);
                ci.cancel();
                return;
            }

            // BaseMonsterEntity 描画
            EntityType<?> type = cap.getTransformedMobId() != null
                    ? EntityType.byString(cap.getTransformedMobId().toString()).orElse(null)
                    : null;
            if (type == null) return;

            UUID playerId = player.getUUID();
            LivingEntity cachedEntity = playerEntityCache.get(playerId);

            if (cachedEntity == null || cachedEntity.getType() != type) {
                Entity entity = type.create(player.level());
                if (!(entity instanceof BaseMonsterEntity<?> living)) return;
                cachedEntity = living;
                playerEntityCache.put(playerId, cachedEntity);
            }

            // プレイヤーの体回転を反映
            cachedEntity.setYRot(player.getYRot());
            cachedEntity.yBodyRot = player.yBodyRot;

            // 首がある場合のみ首回転を反映
            if (cachedEntity instanceof BaseMonsterEntity<?> baseMonster) {
                if (!baseMonster.isHeadless()) { // isHeadless は各モンスターで定義
                    cachedEntity.setYHeadRot(player.getYHeadRot());
                }
            }

            // アニメーションロック中は walkAnimation 更新を抑制
            if (cachedEntity instanceof BaseMonsterEntity<?> baseMonster && !baseMonster.isAnimationLocked()) {
                cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
                cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());
            }

            // GeckoLib 描画
            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(cachedEntity)
                    .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

            ci.cancel();
        });
    }
}
