package net.mimic.monstermod.event;

import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;

public class CapabilityEvents {

    // プレイヤーにCapabilityを付与
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).isPresent()) {
                event.addCapability(
                        new ResourceLocation(MonsterMod.MOD_ID, "transformation"),
                        new PlayerTransformationProvider()
                );
            }
        }
    }

    // プレイヤークローン時（リスポーンや死亡復活など）
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(oldCap -> {
            event.getEntity().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(newCap -> {
                newCap.setTransformed(oldCap.isTransformed());
                newCap.setTransformedMobId(oldCap.getTransformedMobId());
            });
        });
    }

    // ログイン時に変身状態を送信
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                        transformation.isTransformed(),
                        transformation.getTransformedMobId()
                ), player);
            });
        }
    }

    // リスポーン時に変身状態を送信
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                        transformation.isTransformed(),
                        transformation.getTransformedMobId()
                ), player);
            });
        }
    }

    // ゲームイベントバスに登録するためのメソッド
    public static void register() {
        MinecraftForge.EVENT_BUS.register(CapabilityEvents.class);
    }
}
