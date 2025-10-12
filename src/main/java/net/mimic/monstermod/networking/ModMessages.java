package net.mimic.monstermod.networking;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.networking.client.C2SMonsterStatePacket;
import net.mimic.monstermod.networking.server.S2CMonsterSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;

public class ModMessages {

    public static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MonsterMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        // クライアント → サーバー
        INSTANCE.messageBuilder(C2SMonsterStatePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SMonsterStatePacket::encode)
                .decoder(C2SMonsterStatePacket::decode)
                .consumerMainThread(C2SMonsterStatePacket::handle)
                .add();

        // サーバー → クライアント
        INSTANCE.messageBuilder(S2CMonsterSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CMonsterSyncPacket::encode)
                .decoder(S2CMonsterSyncPacket::decode)
                .consumerMainThread(S2CMonsterSyncPacket::handle)
                .add();
    }

    // ----------------------------
    // 共通ユーティリティ
    // ----------------------------
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** ★ 全クライアントに送信する */
    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    /** サーバーに送信する（クライアント側から呼び出し） */
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
