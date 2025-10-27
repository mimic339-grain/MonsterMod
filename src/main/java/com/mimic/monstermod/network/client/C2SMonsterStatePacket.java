package com.mimic.monstermod.network.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMonsterSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SMonsterStatePacket {
    private final int entityId;
    private final String animation;
    private final String skill;

    public C2SMonsterStatePacket(int entityId, String animation, String skill) {
        this.entityId = entityId;
        this.animation = animation;
        this.skill = skill;
    }

    public static void encode(C2SMonsterStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.animation);
        buf.writeUtf(msg.skill);
    }

    public static C2SMonsterStatePacket decode(FriendlyByteBuf buf) {
        return new C2SMonsterStatePacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(C2SMonsterStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.level() == null) return;

            // サーバー受信時に全クライアントに送信（送信者も含む場合は除外しない）
            S2CMonsterSyncPacket sync = new S2CMonsterSyncPacket(
                    msg.entityId,
                    msg.animation,
                    msg.skill
            );

            ModMessages.sendToAllClients(sync); // 送信者も含めて全員同期
        });
        ctx.get().setPacketHandled(true);
    }
}
