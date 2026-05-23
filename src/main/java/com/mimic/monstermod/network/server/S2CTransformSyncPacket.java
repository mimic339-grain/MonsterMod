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

    // S2CTransformSyncPacket.java の handle メソッド内

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Player player = Minecraft.getInstance().level.getPlayerByUUID(playerId);
            if (player == null) return;

            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                // 1. データを流し込む（isTransformed や transformedMobId がセットされる）
                trans.deserializeNBT(nbt);

                // 2. ★ここが重要：見た目とIdentityのインスタンスをクライアント側でも即座に作る
                trans.onLoad(player);

                // 3. 属性とHPの適用
                MonsterTransformUtil.applyFullTransformation(player, trans);

                // 4. HPの最終補正
                if (trans.isTransformed() && trans.getIdentity() != null) {
                    if (nbt.contains("identityHP")) {
                        double targetHP = nbt.getDouble("identityHP");
                        MonsterTransformUtil.setIdentityHP(player, trans.getIdentity().getId(), targetHP);
                        player.setHealth((float) Math.min(targetHP, player.getMaxHealth()));
                    }
                } else if (nbt.contains("playerHP")) {
                    double targetHP = nbt.getDouble("playerHP");
                    MonsterTransformUtil.setPlayerHP(player, targetHP);
                    player.setHealth((float) Math.min(targetHP, player.getMaxHealth()));
                }

                player.refreshDimensions();
            });
        });
        ctx.get().setPacketHandled(true);
    }
}