package com.mimic.monstermod.network.server;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client
 * HunterTransformation 同期パケット
 * ・サーバー authoritative
 * ・Client 側 Capability を完全同期
 * ・Layer は Capability を参照して自動更新
 */
public class S2CHunterSyncPacket {

    private final UUID playerId;
    private final CompoundTag nbt;

    public S2CHunterSyncPacket(UUID playerId, CompoundTag nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    // ------------------------------------------------
    // Codec
    // ------------------------------------------------
    public static void encode(S2CHunterSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    public static S2CHunterSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CHunterSyncPacket(
                buf.readUUID(),
                buf.readNbt()
        );
    }

    // ------------------------------------------------
    // Client Handle
    // ------------------------------------------------
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            Player player = mc.level.getPlayerByUUID(playerId);
            if (player == null) {
                return;
            }

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(hunter -> {

                        // ---- APPLY ----
                        hunter.deserializeNBT(nbt);
                        hunter.onLoad(player); // client-only補正（Hotbarなど）

                    });
        });

        ctx.get().setPacketHandled(true);
    }
}
