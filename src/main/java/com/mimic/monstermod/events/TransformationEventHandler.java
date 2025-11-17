package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class TransformationEventHandler {

    // --------------------------------
    // ■ 死亡イベント（正しい 1.20.1）
    // --------------------------------
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            try {
                // ★identityHP を 0 にするなど死亡処理
                cap.onPlayerDeath(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    // --------------------------------
    // ■ リスポーンイベント（正しい 1.20.1）
    //   PlayerEvent.Clone が正しい
    // --------------------------------
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer))
            return;

        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();

        oldPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(oldCap -> {
            newPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(newCap -> {

                // ★ UUID を newCap に渡して copyFrom
                newCap.copyFrom(oldCap, oldPlayer.getUUID());

                // ★ identityHP のゼロ値補正
                newCap.onPlayerRespawn(newPlayer);
            });
        });
    }
}
