package com.mimic.monstermod.network.server;

import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final UUID playerId;
    private final CompoundTag nbt;

    public S2CTransformSyncPacket(UUID playerId, CompoundTag nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    public static void encode(S2CTransformSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    public static S2CTransformSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CTransformSyncPacket(buf.readUUID(), buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = Minecraft.getInstance().level != null ?
                    Minecraft.getInstance().level.getPlayerByUUID(playerId) : null;
            if (player == null) return;

            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                // 1. データの同期
                trans.deserializeNBT(nbt);

                // 2. クライアント側で実体（Entity）を強制生成
                // これをやらないと getEntity() が null を返し、 Steve サイズになる
                trans.onLoad(player);

                // 3. HP情報の適用
                if (nbt.contains("playerHP")) MonsterTransformUtil.setPlayerHP(player, nbt.getDouble("playerHP"));
                if (nbt.contains("identityHP") && trans.getMobId() != null) {
                    MonsterTransformUtil.setIdentityHP(player, trans.getMobId().toString(), nbt.getDouble("identityHP"));
                }

                // 4. 当たり判定・目線の更新
                MonsterTransformUtil.applyFullTransformation(player, trans);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}