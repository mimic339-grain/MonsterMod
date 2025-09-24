package net.mimic.monstermod.networking;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket;
import net.mimic.monstermod.networking.packet.PlayerTransformC2SPacket;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MonsterMod.MOD_ID, "main_channel"),
                () -> "1.0",
                (version) -> true,
                (version) -> true
        );

        MonsterMod.CHANNEL = INSTANCE; // ← これを追加する！！

        INSTANCE.messageBuilder(PlayerTransformC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlayerTransformC2SPacket::new)
                .encoder(PlayerTransformC2SPacket::encode)
                .consumerMainThread(PlayerTransformC2SPacket::handle)
                .add();

        INSTANCE.messageBuilder(MimicSwitchC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MimicSwitchC2SPacket::new)
                .encoder(MimicSwitchC2SPacket::encode)
                .consumerMainThread(MimicSwitchC2SPacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CTransformSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CTransformSyncPacket::decode)
                .encoder(S2CTransformSyncPacket::encode)
                .consumerMainThread(S2CTransformSyncPacket::handle)
                .add();
    }

    public static SimpleChannel getChannel() {
        return INSTANCE;
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
