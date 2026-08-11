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
 * HunterCombatState(納刀/コンボ/硬直)を対象プレイヤーのUUID付きで同期するパケット。
 *
 * 【注意】以前は対象プレイヤーを指定せず常にMinecraft.getInstance().player
 * (受信したクライアント自身)に適用していたため、TRACKING_ENTITY_AND_SELFで
 * ブロードキャストすると、他プレイヤーが自分自身の戦闘状態を誤って上書きして
 * しまう不具合があった。S2CTransformSyncPacketと同じくUUIDで対象を解決する。
 */
public class S2C_SyncCombatStatePacket {

    private final UUID playerId;
    private final CompoundTag data;

    public S2C_SyncCombatStatePacket(UUID playerId, CompoundTag tag) {
        this.playerId = playerId;
        this.data = tag;
    }

    public static void encode(S2C_SyncCombatStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.data);
    }

    public static S2C_SyncCombatStatePacket decode(FriendlyByteBuf buf) {
        return new S2C_SyncCombatStatePacket(buf.readUUID(), buf.readNbt());
    }

    public static void handle(S2C_SyncCombatStatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Player player = Minecraft.getInstance().level.getPlayerByUUID(msg.playerId);
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_COMBAT_STATE)
                    .ifPresent(cap -> cap.deserializeNBT(msg.data));
        });

        ctx.setPacketHandled(true);
    }
}
