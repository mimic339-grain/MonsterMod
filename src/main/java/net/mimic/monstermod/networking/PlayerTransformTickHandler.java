package net.mimic.monstermod.networking;

import net.mimic.monstermod.MonsterMod;
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

    // ------------------------
    // サーバ側Tick：位置・回転を補間してプレイヤーに追従
    // ------------------------
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (Map.Entry<UUID, BaseMonsterEntity<?>> entry : transformedPlayers.entrySet()) {
            BaseMonsterEntity<?> entity = entry.getValue();
            if (entity == null || entity.isRemoved()) continue;

            if (!(entity.level() instanceof ServerLevel serverLevel)) continue;
            Player player = serverLevel.getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            // 速度コピー
            entity.setDeltaMovement(player.getDeltaMovement());

            double lerp = 0.5;
            // 位置補間
            entity.setPos(
                    entity.getX() + (player.getX() - entity.getX()) * lerp,
                    entity.getY() + (player.getY() - entity.getY()) * lerp,
                    entity.getZ() + (player.getZ() - entity.getZ()) * lerp
            );

            // 回転補間：SynchedEntityData にセット
            if (entity instanceof MimicEntity mimic) {
                mimic.setBodyRot(lerpRotation(mimic.getBodyRot(), player.yBodyRot, lerp));
                mimic.setHeadRot(lerpRotation(mimic.getHeadRot(), player.yHeadRot, lerp));
            } else {
                entity.yBodyRot = lerpRotation(entity.yBodyRot, player.yBodyRot, lerp);
                entity.yHeadRot = lerpRotation(entity.yHeadRot, player.yHeadRot, lerp);
            }

            // 前フレーム値
            entity.yBodyRotO = entity.yBodyRot;
            entity.yHeadRotO = entity.yHeadRot;

            entity.tick();
        }
    }

    private static float lerpRotation(float from, float to, double lerp) {
        float delta = (to - from) % 360;
        if (delta < -180) delta += 360;
        if (delta >= 180) delta -= 360;
        return from + (float)(delta * lerp);
    }

    // ------------------------
    // 変身処理
    // ------------------------
    public static void handleTransformRequest(ServerPlayer player, boolean transform, net.minecraft.resources.ResourceLocation identityId) {
        if (player == null) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {

            if (!transform) {
                BaseMonsterEntity<?> entity = cap.getTransformedEntity();
                if (entity != null) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    removeTransformed(player);
                }

                cap.setTransformed(false);
                cap.setTransformedMobId(null);
                cap.setTransformedEntity(null);
                cap.syncToClient(player);

                MonsterMod.getLogger().debug("{} の変身を解除", player.getName().getString());
                return;
            }

            if (identityId == null) return;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(identityId);
            if (type == null) return;

            Entity entity = type.create(player.level());
            if (!(entity instanceof BaseMonsterEntity<?> monster)) return;

            monster.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.level().addFreshEntity(monster);

            cap.setTransformed(true);
            cap.setTransformedMobId(identityId);
            cap.setTransformedEntity(monster);

            registerTransformed(player, monster);
            cap.syncToClient(player);

            MonsterMod.getLogger().debug("{} を {} に変身させました", player.getName().getString(), identityId);
        });
    }

    // ------------------------
    // クライアント側Tick：位置補間＋回転補間＋アニメーション更新
    // ------------------------
    @Mod.EventBusSubscriber(Dist.CLIENT)
    public static class ClientTickHandler {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (!transformation.isTransformed()) return;

                Entity entity = transformation.getClientTransformedEntity();
                Level level = player.level();
                if (level == null) return;

                MimicEntity mimic;
                if (!(entity instanceof MimicEntity)) {
                    mimic = new MimicEntity(
                            (EntityType<? extends BaseMonsterEntity<?>>) (EntityType<?>) MonsterMod.getMimicEntityType(),
                            level
                    );
                    mimic.linkToPlayer(player.getUUID().toString());
                    transformation.setClientTransformedEntity(mimic);

                    // 初期位置・回転コピー
                    mimic.setPos(player.getX(), player.getY(), player.getZ());
                    mimic.setBodyRot(player.yBodyRot);
                    mimic.setHeadRot(player.yHeadRot);
                    mimic.yBodyRot = mimic.getBodyRot();
                    mimic.yHeadRot = mimic.getHeadRot();
                    mimic.yBodyRotO = mimic.yBodyRot;
                    mimic.yHeadRotO = mimic.yHeadRot;
                } else {
                    mimic = (MimicEntity) entity;
                }

                // 前フレーム値を保持
                mimic.yBodyRotO = mimic.yBodyRot;
                mimic.yHeadRotO = mimic.yHeadRot;

                // サーバが更新した値を取得
                float targetBodyRot = mimic.getBodyRot();
                float targetHeadRot = mimic.getHeadRot();

                // 補間して回転を更新
                float lerp = 0.5f;
                mimic.yBodyRot += (targetBodyRot - mimic.yBodyRot) * lerp;
                mimic.yHeadRot += (targetHeadRot - mimic.yHeadRot) * lerp;

                // アニメーション更新
                mimic.updateAnimationStateClient();
            });
        }
    }
}
