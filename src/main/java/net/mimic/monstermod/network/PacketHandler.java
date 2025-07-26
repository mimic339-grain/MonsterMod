package net.mimic.monstermod.network;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.network.morph.MorphSyncPacket; // 登録するカスタムパケット
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
// 以下のForgeネットワーク関連のインポートパスを修正
import net.minecraftforge.network.PacketDistributor; // これまで通り
import net.minecraftforge.network.simple.SimpleChannel; // 新しいチャンネルクラス
import net.minecraftforge.network.NetworkRegistry; // チャンネル登録用クラス

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    // Mod専用のネットワークチャンネルを定義
    // SimpleChannel を使用し、NetworkRegistry.new -> NetworkRegistry.newSimpleChannel に変更
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MonsterMod.MOD_ID, "main"), // チャンネル名
            () -> PROTOCOL_VERSION, // サーバーが受け入れるプロトコルバージョン
            PROTOCOL_VERSION::equals, // クライアントが受け入れるプロトコルバージョン
            PROTOCOL_VERSION::equals // サーバーが受け入れるプロトコルバージョン
    );

    // Modのロード時に呼び出され、全てのカスタムメッセージを登録する
    public static void registerMessages() {
        int id = 0; // 各メッセージにユニークなIDを割り当てる

        // MorphSyncPacket (変身同期パケット) を登録
        // 方向: サーバーからクライアントへ (NetworkDirection.PLAY_TO_CLIENT)
        // encoder, decoder, consumer は変更なし
        INSTANCE.messageBuilder(MorphSyncPacket.class, id++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MorphSyncPacket::encode)
                .decoder(MorphSyncPacket::new)
                .consumerMainThread(MorphSyncPacket::handle) // メインスレッドで処理を実行
                .add();

        // 追記：もし他のC2S/S2Cパケットを登録するならここに追加
        // PacketDistributor.SERVER.noArg() はクライアントからサーバーへの送信
        // PacketDistributor.PLAYER.with(() -> player) はサーバーから特定のプレイヤーへの送信
    }

    // 特定のサーバープレイヤーにメッセージを送信するヘルパーメソッド
    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    // サーバー上のエンティティをトラッキングしているクライアントと、そのエンティティ自体にメッセージを送信する
    public static void sendToClientsTrackingAndSelf(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }

    // サーバーにメッセージを送信するヘルパーメソッド (クライアント側から呼び出される)
    public static void sendToServer(Object message) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
    }
}