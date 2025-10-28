package com.mimic.monstermod.network.server;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * YSMMOD方式: クライアントの Identity 側に NBT を反映する
 */
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
        ctx.get().enqueueWork(this::syncClient);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void syncClient() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    BaseMonsterIdentity identity = trans.getIdentity();
                    if (identity != null) {
                        identity.deserializeNBT(tag);
                    }
                });
    }
}
