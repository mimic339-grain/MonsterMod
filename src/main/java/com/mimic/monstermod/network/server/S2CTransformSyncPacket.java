package com.mimic.monstermod.network.server;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー → クライアント間の変身状態同期パケット。
 * NBT を使って PlayerTransformation の状態を丸ごと同期する。
 */
public class S2CTransformSyncPacket {
    private final UUID playerUUID;
    private final net.minecraft.nbt.CompoundTag nbt;

    // ========================
    // コンストラクタ
    // ========================
    public S2CTransformSyncPacket(UUID playerUUID, net.minecraft.nbt.CompoundTag nbt) {
        this.playerUUID = playerUUID;
        this.nbt = nbt;
    }

    // ========================
    // デコード / エンコード
    // ========================
    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeNbt(nbt);
    }

    // ========================
    // ハンドラー
    // ========================
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(this::handleClient);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player targetPlayer = mc.level.getPlayerByUUID(playerUUID);
        if (targetPlayer == null) {
            MonsterMod.getLogger().warn("[S2CTransformSyncPacket] Client: 対象プレイヤーが見つかりません UUID={}", playerUUID);
            return;
        }

        // PlayerTransformationProvider の NBT を直接読み込む
        targetPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            cap.deserializeNBT(nbt);

            // クライアント側エンティティ処理
            if (!cap.get().isTransformed()) {
                if (cap.get().getTransformedEntity(mc.level) != null) {
                    cap.get().getTransformedEntity(mc.level).discard();
                }
                MonsterMod.getLogger().debug("[S2CTransformSyncPacket] Client: 変身解除 {}", targetPlayer.getName().getString());
            } else {
                var entity = cap.get().getTransformedEntity(mc.level);
                if (entity != null) {
                    entity.setPos(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                    MonsterMod.getLogger().debug("[S2CTransformSyncPacket] Client: 変身同期 {} -> mobId={}", targetPlayer.getName().getString(), cap.get().getTransformedMobId());
                } else {
                    MonsterMod.getLogger().warn("[S2CTransformSyncPacket] Client: Entity生成失敗 mobId={}", cap.get().getTransformedMobId());
                }
            }
        });
    }
}
