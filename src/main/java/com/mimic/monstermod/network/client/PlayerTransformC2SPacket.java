package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー 変身リクエストパケット（YSMMOD方式）
 * - mobId == null の場合は変身解除
 * - サーバーで Capability を更新し、全クライアントに同期
 */
public class PlayerTransformC2SPacket {

    private final ResourceLocation mobId;

    // ========================
    // コンストラクタ
    // ========================
    public PlayerTransformC2SPacket(ResourceLocation mobId) {
        this.mobId = mobId; // null なら解除
    }

    // ========================
    // デコード / エンコード
    // ========================
    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.mobId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(mobId != null);
        if (mobId != null) buf.writeResourceLocation(mobId);
    }

    // ========================
    // ハンドラー（サーバー側）
    // ========================
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(cap -> {
                        if (mobId != null) {
                            // 変身リクエスト
                            if (!BaseMonsterIdentityRegistry.hasIdentity(mobId)) {
                                // 未登録IDは無視
                                return;
                            }
                            if (!cap.isTransformed()) {
                                cap.setTransformed(true);
                                cap.setTransformedMobId(mobId);
                                cap.startTransformation(player, mobId);
                            }
                        } else {
                            // 変身解除
                            if (cap.isTransformed()) {
                                cap.setTransformed(false);
                                cap.setTransformedMobId(null);
                                cap.stopTransformation(player);
                            }
                        }

                        // 状態同期（必須）
                        cap.syncToClient(player);
                    });
        });
        ctx.get().setPacketHandled(true);
    }

    // ========================
    // Getter
    // ========================
    public ResourceLocation getMobId() {
        return mobId;
    }
}
