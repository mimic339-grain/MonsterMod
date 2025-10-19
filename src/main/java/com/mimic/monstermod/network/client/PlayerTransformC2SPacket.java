package com.mimic.monstermod.network.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー 変身リクエストパケット。
 * - クライアントが mobId を指定して送信。
 * - サーバー側で PlayerTransformation を更新し、全クライアントへ同期。
 */
public class PlayerTransformC2SPacket {

    private final ResourceLocation mobId;
    private final boolean requestTransform;

    // ========================
    // コンストラクタ
    // ========================
    public PlayerTransformC2SPacket(ResourceLocation mobId, boolean requestTransform) {
        this.mobId = mobId;
        this.requestTransform = requestTransform;
    }

    // ========================
    // デコード / エンコード
    // ========================
    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.requestTransform = buf.readBoolean();
        this.mobId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(requestTransform);
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
                        if (requestTransform) {
                            // 変身リクエスト
                            if (mobId == null) {
                                MonsterMod.getLogger().warn("[C2SPacket] mobId が null のため無視");
                                return;
                            }

                            if (!BaseMonsterIdentityRegistry.hasIdentity(mobId)) {
                                MonsterMod.getLogger().warn("[C2SPacket] 未登録のmobId: {}", mobId);
                                return;
                            }

                            if (cap.isTransformed()) {
                                MonsterMod.getLogger().debug("[C2SPacket] {} は既に変身中です", player.getName().getString());
                                return;
                            }

                            // 変身開始
                            cap.setTransformed(true);
                            cap.setTransformedMobId(mobId);
                            cap.startTransformation(player, mobId);
                            MonsterMod.getLogger().info("[C2SPacket] {} が変身: {}", player.getName().getString(), mobId);

                        } else {
                            // 変身解除
                            if (!cap.isTransformed()) {
                                MonsterMod.getLogger().debug("[C2SPacket] {} は変身していません", player.getName().getString());
                                return;
                            }

                            cap.setTransformed(false);
                            cap.setTransformedMobId(null);
                            cap.stopTransformation(player);
                            MonsterMod.getLogger().info("[C2SPacket] {} が変身解除", player.getName().getString());
                        }

                        // 状態同期
                        cap.syncToClient(player);
                    });
        });
        ctx.get().setPacketHandled(true);
    }
}
