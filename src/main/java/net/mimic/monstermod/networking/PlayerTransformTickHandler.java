package net.mimic.monstermod.networking;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (Map.Entry<UUID, BaseMonsterEntity<?>> entry : transformedPlayers.entrySet()) {
            BaseMonsterEntity<?> entity = entry.getValue();
            if (entity == null || entity.isRemoved()) continue;

            Player player = entity.level().getPlayerByUUID(entry.getKey());
            if (player == null) continue;

            // サーバ側では回転補間はせず、位置のみ補間
            double lerp = 0.5;
            entity.setPos(
                    entity.getX() + (player.getX() - entity.getX()) * lerp,
                    entity.getY() + (player.getY() - entity.getY()) * lerp,
                    entity.getZ() + (player.getZ() - entity.getZ()) * lerp
            );

            // クライアントに回転同期用に BODY_ROT / HEAD_ROT をセット
            if (entity instanceof MimicEntity mimic) {
                mimic.setBodyRot(player.yBodyRot);
                mimic.setHeadRot(player.yHeadRot);
            }

            entity.tick();
        }
    }
}
