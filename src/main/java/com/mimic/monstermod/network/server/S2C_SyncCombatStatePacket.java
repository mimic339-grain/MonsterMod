package com.mimic.monstermod.network.server;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2C_SyncCombatStatePacket {

    private final CompoundTag data;

    public S2C_SyncCombatStatePacket(CompoundTag tag) {
        this.data = tag;
    }

    public S2C_SyncCombatStatePacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_COMBAT_STATE)
                    .ifPresent(cap -> cap.deserializeNBT(data));
        });

        ctx.setPacketHandled(true);
    }
}
