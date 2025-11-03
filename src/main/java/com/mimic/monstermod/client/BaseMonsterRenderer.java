package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BaseMonsterRenderer 完全版
 * - BaseMonsterIdentity に基づく描画とアニメーション補間
 * - ClientCacheManager による強参照キャッシュ
 * - PlayerRenderer Mixin と統合可能
 */
@OnlyIn(Dist.CLIENT)
public class BaseMonsterRenderer<T extends BaseMonsterEntity> extends EntityRenderer<T> {

    private static final ResourceLocation DEFAULT_TEXTURE =
            new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");

    public BaseMonsterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        BaseMonsterIdentity identity = ClientCacheManager.getIdentity(entity);
        if (identity == null) return;

        // モデル初期化
        if (entity.getModelRoot() == null) entity.ensureModelInitialized();
        if (entity.getModelRoot() == null) return;

        poseStack.pushPose();
        try {
            // Y軸回転
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

            // Identity 側でアニメーション補間と描画
            identity.renderInterpolated(entity, partialTicks, poseStack, buffer, packedLight);

        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public @Nullable ResourceLocation getTextureLocation(T entity) {
        BaseMonsterIdentity identity = ClientCacheManager.getIdentity(entity);
        if (identity != null && identity.getTexture() != null) return identity.getTexture();
        return DEFAULT_TEXTURE;
    }

    /** キャッシュ管理 */
    @OnlyIn(Dist.CLIENT)
    public static class ClientCacheManager {

        private static final Map<UUID, BaseMonsterIdentity> identityCache = new ConcurrentHashMap<>();

        /** ワールド入場時に全プレイヤー Identity をプリロード */
        public static void preloadAllPlayersIdentities() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            for (AbstractClientPlayer player : mc.level.players()) preloadIdentity(player);
        }

        /** プレイヤーの Identity プリロード */
        public static void preloadIdentity(AbstractClientPlayer player) {
            UUID uuid = player.getUUID();
            if (identityCache.containsKey(uuid)) return;

            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(pt -> {
                        if (pt.isTransformed() && pt.getIdentity() != null) {
                            BaseMonsterIdentity identity = pt.getIdentity();
                            identity.setEntity(pt.getEntity());
                            identity.autoInitBoneMap(pt.getEntity());
                            identityCache.put(uuid, identity);
                            MonsterMod.LOGGER.debug("[ClientCacheManager] Preloaded Identity for UUID={}", uuid);
                        }
                    });
        }

        /** 描画時キャッシュ参照 */
        @Nullable
        public static BaseMonsterIdentity getIdentity(BaseMonsterEntity entity) {
            if (entity == null) return null;
            BaseMonsterIdentity identity = identityCache.get(entity.getUUID());
            if (identity != null && identity.getEntity() == entity) return identity;
            return null;
        }

        /** 新規プレイヤー出現時に自動プリロード */
        public static void onPlayerSpawned(AbstractClientPlayer player) {
            preloadIdentity(player);
        }
    }

    /** Forge イベントでプリロード管理 */
    @Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {

        /** クライアントセットアップ時に全プレイヤープリロード */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            Minecraft.getInstance().execute(ClientCacheManager::preloadAllPlayersIdentities);
        }

        /** 新規プレイヤー出現時に自動プリロード */
        @SubscribeEvent
        public static void onPlayerJoin(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof AbstractClientPlayer player) {
                ClientCacheManager.onPlayerSpawned(player);
            }
        }
    }

}
