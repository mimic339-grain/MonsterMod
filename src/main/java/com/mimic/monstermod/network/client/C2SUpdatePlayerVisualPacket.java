package com.mimic.monstermod.network.client;

import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SUpdatePlayerVisualPacket {
    private final boolean show;
    private final float r, g, b, thickness; // ★ thickness追加

    public C2SUpdatePlayerVisualPacket(boolean show, float r, float g, float b, float thickness) {
        this.show = show;
        this.r = r;
        this.g = g;
        this.b = b;
        this.thickness = thickness;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(show);
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeFloat(thickness); // ★ 送信
    }

    public static C2SUpdatePlayerVisualPacket decode(FriendlyByteBuf buf) {
        return new C2SUpdatePlayerVisualPacket(buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
                cap.setState(IPlayerData.STATE_SHOW_SKILL_LEAD, show);
                cap.setLeadColor(r, g, b);
                cap.setLeadThickness(thickness); // ★ サーバー側に保存
                CapabilityRegistry.syncToClient(player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}