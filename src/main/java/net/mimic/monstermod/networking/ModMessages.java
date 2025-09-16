package net.mimic.monstermod.networking;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket;
import net.mimic.monstermod.networking.packet.PlayerTransformC2SPacket;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.mimic.monstermod.networking.packet.SyncMimicRotationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {

    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MonsterMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        // クライアント → サーバ
        INSTANCE.messageBuilder(PlayerTransformC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlayerTransformC2SPacket::new)
                .encoder(PlayerTransformC2SPacket::encode)
                .consumerMainThread(PlayerTransformC2SPacket::handle)
                .add();

        INSTANCE.messageBuilder(MimicSwitchC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(MimicSwitchC2SPacket::new)
                .encoder(MimicSwitchC2SPacket::encode)
                .consumerMainThread(MimicSwitchC2SPacket::handle)
                .add();

        // サーバ → クライアント
        INSTANCE.messageBuilder(S2CTransformSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CTransformSyncPacket::new)
                .encoder(S2CTransformSyncPacket::encode)
                .consumerMainThread(S2CTransformSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncMimicRotationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncMimicRotationPacket::new)
                .encoder(SyncMimicRotationPacket::encode)
                .consumerMainThread(SyncMimicRotationPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player != null) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}
