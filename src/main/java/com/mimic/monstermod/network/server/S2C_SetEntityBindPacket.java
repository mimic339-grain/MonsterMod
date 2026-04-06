package com.mimic.monstermod.network.server;

import com.mimic.monstermod.effect.EffectClientVariables;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class S2C_SetEntityBindPacket {
    private final boolean isBinded;

    public S2C_SetEntityBindPacket(boolean isBinded) {
        this.isBinded = isBinded;
    }

    public static void encode(S2C_SetEntityBindPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.isBinded);
    }

    public static S2C_SetEntityBindPacket decode(FriendlyByteBuf buf) {
        return new S2C_SetEntityBindPacket(buf.readBoolean());
    }

    public static void handle(S2C_SetEntityBindPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            // クライアント側の変数に保存（後述の ClientVariables 等）
            EffectClientVariables.isBinded = pkt.isBinded;
        });
        ctx.setPacketHandled(true);
    }
}