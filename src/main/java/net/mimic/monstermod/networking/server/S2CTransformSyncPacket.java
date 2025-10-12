package net.mimic.monstermod.networking.server;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー → クライアント間の変身状態同期パケット。
 * プレイヤーの変身状態（mobId / isTransformed）をクライアントに反映させる。
 */
public class S2CTransformSyncPacket {
    private final UUID playerUUID;
    private final ResourceLocation transformedMobId;
    private final boolean isTransformed;

    // ========================
    // コンストラクタ
    // ========================
    public S2CTransformSyncPacket(UUID playerUUID, ResourceLocation mobId, boolean isTransformed) {
        this.playerUUID = playerUUID;
        this.transformedMobId = mobId;
        this.isTransformed = isTransformed;
    }

    // ========================
    // デコード / エンコード
    // ========================
    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(isTransformed);
        buf.writeBoolean(transformedMobId != null);
        if (transformedMobId != null) buf.writeResourceLocation(transformedMobId);
    }

    // ========================
    // ハンドラー
    // ========================
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient());
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

        targetPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
            cap.setTransformed(isTransformed);
            cap.setTransformedMobId(transformedMobId);

            if (!isTransformed) {
                // 変身解除：クライアント側エンティティ破棄
                if (cap.getTransformedEntity(mc.level) != null) {
                    cap.getTransformedEntity(mc.level).discard();
                }
                MonsterMod.getLogger().debug("[S2CTransformSyncPacket] Client: 変身解除 {} -> null", targetPlayer.getName().getString());
            } else {
                // 変身開始：クライアント描画用エンティティ作成
                var entity = cap.getTransformedEntity(mc.level);
                if (entity != null) {
                    entity.setPos(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                    MonsterMod.getLogger().debug("[S2CTransformSyncPacket] Client: 変身同期 {} -> mobId={}", targetPlayer.getName().getString(), transformedMobId);
                } else {
                    MonsterMod.getLogger().warn("[S2CTransformSyncPacket] Client: Entity生成失敗 mobId={}", transformedMobId);
                }
            }
        });
    }
}
