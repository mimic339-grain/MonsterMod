package net.mimic.monstermod.networking;

import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Mod.EventBusSubscriber
public class PlayerTransformTickHandler {

    private static final Map<UUID, BaseMonsterEntity<?>> transformedPlayers = new ConcurrentHashMap<>();

    public static void registerTransformed(ServerPlayer player, BaseMonsterEntity<?> entity) {
        if (player != null && entity != null) {
            transformedPlayers.put(player.getUUID(), entity);
        }
    }

    public static void removeTransformed(ServerPlayer player) {
        if (player != null) {
            transformedPlayers.remove(player.getUUID());
        }
    }

    // サーバTick：位置補間のみ
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (Map.Entry<UUID, BaseMonsterEntity<?>> entry : transformedPlayers.entrySet()) {
            BaseMonsterEntity<?> entity = entry.getValue();
            if (entity == null || entity.isRemoved()) continue;

            if (!(entity.level() instanceof ServerLevel serverLevel)) continue;
            Player player = serverLevel.getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            double lerp = 0.5;
            entity.setPos(
                    entity.getX() + (player.getX() - entity.getX()) * lerp,
                    entity.getY() + (player.getY() - entity.getY()) * lerp,
                    entity.getZ() + (player.getZ() - entity.getZ()) * lerp
            );

            entity.tick();
        }
    }

    // 変身処理（サーバ）
    public static void handleTransformRequest(ServerPlayer player, boolean transform, net.minecraft.resources.ResourceLocation identityId) {
        if (player == null) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {

            if (!transform) {
                int entityId = cap.getTransformedEntityId();
                if (entityId != -1) {
                    Entity e = player.level().getEntity(entityId);
                    if (e != null) e.discard();
                    removeTransformed(player);
                }

                cap.setTransformed(false);
                cap.setTransformedMobId(null);
                cap.setTransformedEntityId(-1);
                cap.syncToClient(player);
                return;
            }

            if (identityId == null) return;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(identityId);
            if (type == null) return;

            Level level = player.level();
            if (level == null) return;

            Entity entity = type.create(level);
            if (!(entity instanceof BaseMonsterEntity<?> monster)) return;

            monster.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            level.addFreshEntity(monster);

            cap.setTransformed(true);
            cap.setTransformedMobId(identityId);
            cap.setTransformedEntityId(monster.getId());

            if (monster instanceof MimicEntity mimic) {
                mimic.yBodyRot = player.yBodyRot;
                mimic.yHeadRot = player.yHeadRot;
            }

            registerTransformed(player, monster);
            cap.syncToClient(player);
        });
    }

    public static PlayerTransformation getOrCreateTransformation(LocalPlayer player) {
        if (player == null) return null;
        return player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
    }

    // クライアントTick：位置補間＋回転補間
    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class ClientTickHandler {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            PlayerTransformation transformation = PlayerTransformTickHandler.getOrCreateTransformation(player);
            if (transformation == null || !transformation.isTransformed()) return;

            int entityId = transformation.getTransformedEntityId();
            if (entityId == -1) return;

            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof MimicEntity mimic)) return;

            // 前フレーム値
            mimic.yBodyRotO = mimic.yBodyRot;
            mimic.yHeadRotO = mimic.yHeadRot;

            // 位置補間
            float lerp = 0.2f;
            mimic.setPos(
                    mimic.getX() + (player.getX() - mimic.getX()) * lerp,
                    mimic.getY() + (player.getY() - mimic.getY()) * lerp,
                    mimic.getZ() + (player.getZ() - mimic.getZ()) * lerp
            );

            // 回転補間（プレイヤー回転に追従）
            mimic.yBodyRot += (player.yBodyRot - mimic.yBodyRot) * lerp;
            mimic.yHeadRot += (player.yHeadRot - mimic.yHeadRot) * lerp;
        }
    }
}
