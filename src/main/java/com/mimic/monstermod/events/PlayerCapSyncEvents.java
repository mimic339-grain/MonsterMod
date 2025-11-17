package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class PlayerCapSyncEvents {

    // ====== PLAYER CLONE ======
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) ||
                !(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        // 1. 全 Cap コピー
        CapabilityRegistry.copyCaps(oldPlayer, newPlayer);

        // 2. PlayerTransformation 専用コピー + identityHP補正
        oldPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(oldCap -> {
            newPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(newCap -> {
                newCap.copyFrom(oldCap, oldPlayer.getUUID());
                MonsterTransformUtil.resetAllIdentityHPs(newPlayer);
            });
        });

        // 3. クライアント同期
        CapabilityRegistry.syncToClient(newPlayer);
    }

    // ====== PLAYER RESPAWN ======
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MonsterTransformUtil.resetAllIdentityHPs(player);
        CapabilityRegistry.syncToClient(player);
    }

    // ====== PLAYER LOGIN ======
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            CapabilityRegistry.syncToClient(player);
    }

    // ====== DIMENSION CHANGE ======
    @SubscribeEvent
    public static void onPlayerDimChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            CapabilityRegistry.syncToClient(player);
    }

    // ====== PLAYER DEATH ======
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MonsterTransformUtil.resetAllIdentityHPs(player);
    }
}
