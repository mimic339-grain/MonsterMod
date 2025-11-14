package com.mimic.monstermod.network;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.client.C2SMonsterStatePacket;
import com.mimic.monstermod.network.client.C2SPlayerInputPacket;
import com.mimic.monstermod.network.client.PlayerTransformC2SPacket;
import com.mimic.monstermod.network.server.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModMessages {

    public static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    // ----------------------------
    // パケット登録
    // ----------------------------
    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MonsterMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        // ここにパケットを追加するだけでOK

        registerMessage(C2SMonsterStatePacket.class, C2SMonsterStatePacket::encode, C2SMonsterStatePacket::decode, C2SMonsterStatePacket::handle, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(PlayerTransformC2SPacket.class, PlayerTransformC2SPacket::toBytes, PlayerTransformC2SPacket::new, PlayerTransformC2SPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2SPlayerInputPacket.class, C2SPlayerInputPacket::encode, C2SPlayerInputPacket::decode, C2SPlayerInputPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(S2CMonsterSyncPacket.class, S2CMonsterSyncPacket::encode, S2CMonsterSyncPacket::decode, S2CMonsterSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CMimicDodgePacket.class, S2CMimicDodgePacket::toBytes, S2CMimicDodgePacket::new, S2CMimicDodgePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CMonsterCapSyncPacket.class, S2CMonsterCapSyncPacket::toBytes, S2CMonsterCapSyncPacket::new, S2CMonsterCapSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CTransformSyncPacket.class, S2CTransformSyncPacket::encode, S2CTransformSyncPacket::decode, S2CTransformSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CPlayerCapSyncPacket.class, S2CPlayerCapSyncPacket::toBytes, S2CPlayerCapSyncPacket::new, S2CPlayerCapSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
    }

    // ----------------------------
    // 共通メソッドで1行登録
    // ----------------------------
    private static <MSG> void registerMessage(
            Class<MSG> type,
            BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf> encoder,
            Function<net.minecraft.network.FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer,
            NetworkDirection direction
    ) {
        INSTANCE.registerMessage(id(), type, encoder, decoder, consumer, Optional.of(direction));
    }

    // ----------------------------
    // 送信ユーティリティ
    // ----------------------------
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendToAllClientsExcept(Object packet, ServerPlayer excluded) {
        if (!(excluded.getCommandSenderWorld() instanceof ServerLevel serverLevel)) return;

        for (Entity player : serverLevel.players()) {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer != excluded) {
                sendToPlayer(packet, serverPlayer);
            }
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        if (!(player instanceof FakePlayer)) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}
