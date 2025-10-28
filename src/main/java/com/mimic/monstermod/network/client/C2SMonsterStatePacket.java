package com.mimic.monstermod.network.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMonsterSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: プレイヤー変身中のアニメーション同期要求
 */
public class C2SMonsterStatePacket {
    private final int entityId;
    private final String animation;

    public C2SMonsterStatePacket(int entityId, String animation) {
        this.entityId = entityId;
        this.animation = animation;
    }

    public static void encode(C2SMonsterStatePacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.animation != null ? msg.animation : "");
    }

    public static C2SMonsterStatePacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new C2SMonsterStatePacket(
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(C2SMonsterStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.level() == null) return;

            // null/空文字チェック
            String anim = msg.animation != null ? msg.animation : "";

            // サーバー側でアニメーションイベントを作成
            S2CMonsterSyncPacket sync = new S2CMonsterSyncPacket(
                    msg.entityId,
                    anim, // そのままマッピング済みアニメーション名を送信
                    null
            );

            // 全クライアントに送信
            ModMessages.sendToAllClients(sync);
        });

        // PacketHandled を必ず true に
        ctx.get().setPacketHandled(true);
    }
}
