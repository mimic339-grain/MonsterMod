package com.mimic.monstermod.network;

import com.mimic.monstermod.MonsterMod;
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

        // パケット登録
        registerMessage(PlayerTransformC2SPacket.class, PlayerTransformC2SPacket::toBytes, PlayerTransformC2SPacket::new, PlayerTransformC2SPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2SPlayerInputPacket.class, C2SPlayerInputPacket::encode, C2SPlayerInputPacket::decode, C2SPlayerInputPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(S2CIdentityAnimSyncPacket.class, S2CIdentityAnimSyncPacket::toBytes, S2CIdentityAnimSyncPacket::new, S2CIdentityAnimSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
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

    /** サーバー→特定プレイヤー */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player == null || player instanceof FakePlayer) return;
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** サーバー→全クライアント */
    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    /** サーバー→全クライアント (除外プレイヤーあり) */
    public static <MSG> void sendToAllClientsExcept(MSG message, ServerPlayer excluded) {
        if (excluded == null) return;
        if (!(excluded.getCommandSenderWorld() instanceof ServerLevel serverLevel)) return;

        for (Entity player : serverLevel.players()) {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer != excluded) {
                sendToPlayer(message, serverPlayer);
            }
        }
    }

    /** クライアント→サーバー */
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    /** サーバー→クライアント (個別) */
    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        sendToPlayer(message, player);
    }
}
