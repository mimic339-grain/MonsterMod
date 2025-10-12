package com.mimic.monstermod.network.server;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CMonsterCapSyncPacket {

    private final CompoundTag tag;

    public S2CMonsterCapSyncPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public S2CMonsterCapSyncPacket(FriendlyByteBuf buf) {
        this.tag = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> syncClient(tag));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void syncClient(CompoundTag tag) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // MONSTER_CAPABILITY を取得して同期
        player.getCapability(CapabilityRegistry.MONSTER_CAPABILITY).ifPresent(cap -> {
            cap.deserializeNBT(tag);
        });
    }
}
