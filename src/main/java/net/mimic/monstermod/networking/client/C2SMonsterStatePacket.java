package net.mimic.monstermod.networking.client;

import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.server.S2CMonsterSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー
 * 「このモンスターがアニメーションやスキル状態を変えた」ことを通知する
 */
public class C2SMonsterStatePacket {
    private final int entityId;
    private final String animation;
    private final String skill;

    public C2SMonsterStatePacket(int entityId, String animation, String skill) {
        this.entityId = entityId;
        this.animation = animation;
        this.skill = skill;
    }

    // ----------------------------
    // 書き込み
    // ----------------------------
    public static void encode(C2SMonsterStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.animation);
        buf.writeUtf(msg.skill);
    }

    // ----------------------------
    // 読み込み
    // ----------------------------
    public static C2SMonsterStatePacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        String animation = buf.readUtf();
        String skill = buf.readUtf();
        return new C2SMonsterStatePacket(id, animation, skill);
    }

    // ----------------------------
    // サーバー側で処理
    // ----------------------------
    public static void handle(C2SMonsterStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.level() == null) return;

            // サーバーで受け取った → 全クライアントに配信
            S2CMonsterSyncPacket sync = new S2CMonsterSyncPacket(
                    msg.entityId,
                    msg.animation,
                    msg.skill,
                    0 // tick同期不要
            );
            ModMessages.sendToAllClients(sync);
        });
        ctx.get().setPacketHandled(true);
    }
}
