package com.mimic.monstermod.events;

import com.mimic.monstermod.capability.PlayerTransformationMixinHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * PlayerTransformation をサーバー側で同期するためのイベント
 */
@Mod.EventBusSubscriber(modid = "monstermod")
public class PlayerTransformationEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerTransformationMixinHelper.syncTransformation(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerTransformationMixinHelper.syncTransformation(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerTransformationMixinHelper.syncTransformation(player);
        }
    }
}
