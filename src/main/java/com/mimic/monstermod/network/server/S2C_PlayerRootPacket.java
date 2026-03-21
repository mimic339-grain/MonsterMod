package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.ClientSkillManager; // ★インポートを変更
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2C_PlayerRootPacket {

    private final UUID playerId;
    private final int durationTicks; // 0なら解除

    public S2C_PlayerRootPacket(UUID playerId, int durationTicks) {
        this.playerId = playerId;
        this.durationTicks = durationTicks;
    }

    public static void encode(S2C_PlayerRootPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeInt(pkt.durationTicks);
    }

    public static S2C_PlayerRootPacket decode(FriendlyByteBuf buf) {
        return new S2C_PlayerRootPacket(buf.readUUID(), buf.readInt());
    }

    public static void handle(S2C_PlayerRootPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientSkillManager.applyRoot(pkt.playerId, pkt.durationTicks);
        });
        ctx.get().setPacketHandled(true);
    }

    public UUID getPlayerId() { return playerId; }
    public int getDurationTicks() { return durationTicks; }
}