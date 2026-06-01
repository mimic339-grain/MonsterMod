package com.mimic.monstermod.network;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.client.*;
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
        registerMessage(C2S_SetWeaponSlotPacket.class, C2S_SetWeaponSlotPacket::encode, C2S_SetWeaponSlotPacket::decode, C2S_SetWeaponSlotPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2SAttackPacket.class, C2SAttackPacket::toBytes, C2SAttackPacket::new, C2SAttackPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(PlayerTransformC2SPacket.class, PlayerTransformC2SPacket::toBytes, PlayerTransformC2SPacket::new, PlayerTransformC2SPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2S_SkillCastRequestPacket.class, C2S_SkillCastRequestPacket::encode, C2S_SkillCastRequestPacket::decode,C2S_SkillCastRequestPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2SMonsterInputPacket.class, C2SMonsterInputPacket::encode, C2SMonsterInputPacket::decode, C2SMonsterInputPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2SHunterInputPacket.class, C2SHunterInputPacket::toBytes, C2SHunterInputPacket::new, C2SHunterInputPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2S_SetHunterSkillPacket.class,  C2S_SetHunterSkillPacket::toBytes,  C2S_SetHunterSkillPacket::new,  C2S_SetHunterSkillPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(C2SMonsterInputPacket.class, C2SMonsterInputPacket::encode, C2SMonsterInputPacket::decode, C2SMonsterInputPacket::handle,NetworkDirection.PLAY_TO_SERVER);
        registerMessage(OBBSyncPacket.class, OBBSyncPacket::encode, OBBSyncPacket::new, OBBSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CMonsterSyncPacket.class, S2CMonsterSyncPacket::encode, S2CMonsterSyncPacket::decode, S2CMonsterSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2C_SpawnSkillLeadPacket.class, S2C_SpawnSkillLeadPacket::encode, S2C_SpawnSkillLeadPacket::decode, S2C_SpawnSkillLeadPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CPlayAnimationPacket.class, S2CPlayAnimationPacket::encode, S2CPlayAnimationPacket::decode, S2CPlayAnimationPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(C2SUpdatePlayerVisualPacket.class, C2SUpdatePlayerVisualPacket::encode, C2SUpdatePlayerVisualPacket::decode, C2SUpdatePlayerVisualPacket::handle, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(S2CTransformSyncPacket.class, S2CTransformSyncPacket::encode, S2CTransformSyncPacket::decode, S2CTransformSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CHunterSyncPacket.class, S2CHunterSyncPacket::encode, S2CHunterSyncPacket::decode, S2CHunterSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2CPlayerCapSyncPacket.class, S2CPlayerCapSyncPacket::toBytes, S2CPlayerCapSyncPacket::new, S2CPlayerCapSyncPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2C_SyncCombatStatePacket.class, S2C_SyncCombatStatePacket::toBytes, S2C_SyncCombatStatePacket::new, S2C_SyncCombatStatePacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2C_SyncWeaponSlotPacket.class, S2C_SyncWeaponSlotPacket::encode, S2C_SyncWeaponSlotPacket::decode, S2C_SyncWeaponSlotPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2C_PlayerRootPacket.class, S2C_PlayerRootPacket::encode, S2C_PlayerRootPacket::decode, S2C_PlayerRootPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(S2C_SetEntityBindPacket.class, S2C_SetEntityBindPacket::encode, S2C_SetEntityBindPacket::decode, S2C_SetEntityBindPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
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
    public static void sendOBBToAll(OBBSyncPacket packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
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

        System.out.println("Sending packet to server: " + message.getClass().getSimpleName());

        INSTANCE.sendToServer(message);
    }
    public static <MSG> void sendToClient(MSG message, ServerPlayer player) {
        if (!(player instanceof FakePlayer)) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}
