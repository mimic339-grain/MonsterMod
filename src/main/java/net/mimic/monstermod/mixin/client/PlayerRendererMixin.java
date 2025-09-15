package net.mimic.monstermod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final Map<UUID, BaseMonsterEntity<?>> playerEntityCache = new ConcurrentHashMap<>();

    private PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks,
                          PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            if (!cap.isTransformed()) return;

            // Identity 描画優先
            IPlayerIdentity identity = cap.getTransformedIdentity();
            if (identity != null) {
                identity.applyAnimationAndRender(
                        player, entityYaw, partialTicks, poseStack, buffer, packedLight,
                        cap.getMonsterState(cap.getTransformedMobId())
                );
                ci.cancel();
                return;
            }

            // BaseMonsterEntity 描画
            if (cap.getTransformedMobId() == null) return;
            EntityType<?> type = EntityType.byString(cap.getTransformedMobId().toString()).orElse(null);
            if (type == null) return;

            UUID playerId = player.getUUID();
            BaseMonsterEntity<?> baseMonster = playerEntityCache.computeIfAbsent(playerId, id -> {
                BaseMonsterEntity<?> entity = (BaseMonsterEntity<?>) type.create(player.level());
                if (entity == null) return null;
                return entity;
            });

            if (baseMonster == null) return;

            // 位置・移動同期
            baseMonster.setPos(player.getX(), player.getY(), player.getZ());
            baseMonster.setDeltaMovement(player.getDeltaMovement());

            // 描画のみ
            @SuppressWarnings("unchecked")
            EntityRenderer<BaseMonsterEntity<?>> renderer =
                    (EntityRenderer<BaseMonsterEntity<?>>) Minecraft.getInstance()
                            .getEntityRenderDispatcher().getRenderer(baseMonster);

            renderer.render(baseMonster, entityYaw, partialTicks, poseStack, buffer, packedLight);
            ci.cancel();
        });
    }
}
