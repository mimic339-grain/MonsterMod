package com.mimic.monstermod.network;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forgeネットワークチャンネル — 全パケットの登録と送信ヘルパー。
 *
 * EFM参考:
 *   - network/EpicFightNetworkManager.java
 *   - PacketDistributor.TRACKING_ENTITY でそのEntityを追跡中の全クライアントに送信
 *
 * パケット一覧:
 *   0: S2CTransformSyncPacket  — Monster変身状態
 *   1: S2CHunterSyncPacket     — Hunter状態・スキルCD
 *   2: C2SSkillCastPacket      — クライアント → サーバー スキル発動
 *   3: S2CSkillStatePacket     — SkillステートマシンのS2C同期
 *   4: OBBSyncPacket           — OBBデータ同期
 *
 * 配置: com/mimic/monstermod/network/ModNetworking.java
 */
public class ModNetworking {

    private static final String PROTOCOL = "2"; // パケット変更時はバージョンを上げる
    public static SimpleChannel CHANNEL;
    private static int packetId = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MonsterMod.MOD_ID, "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        // S→C: Monster変身同期
        CHANNEL.registerMessage(packetId++, S2CTransformSyncPacket.class,
                S2CTransformSyncPacket::encode,
                S2CTransformSyncPacket::decode,
                S2CTransformSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // S→C: Hunter同期
        CHANNEL.registerMessage(packetId++, S2CHunterSyncPacket.class,
                S2CHunterSyncPacket::encode,
                S2CHunterSyncPacket::decode,
                S2CHunterSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // C→S: スキル発動リクエスト
        CHANNEL.registerMessage(packetId++, C2SSkillCastPacket.class,
                C2SSkillCastPacket::encode,
                C2SSkillCastPacket::decode,
                C2SSkillCastPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // S→C: スキルステートマシン同期
        CHANNEL.registerMessage(packetId++, S2CSkillStatePacket.class,
                S2CSkillStatePacket::encode,
                S2CSkillStatePacket::decode,
                S2CSkillStatePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // S→C: OBB同期
        CHANNEL.registerMessage(packetId++, OBBSyncPacket.class,
                OBBSyncPacket::encode,
                OBBSyncPacket::new,
                OBBSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    // ── 送信ヘルパー ────────────────────────────────────────────────────

    /** Entityを追跡中の全クライアントに送信 (EFM: TRACKING_ENTITY パターン) */
    public static void sendToAllTracking(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    /** 特定プレイヤーのみに送信 */
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** 全サーバープレイヤーに送信 */
    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}