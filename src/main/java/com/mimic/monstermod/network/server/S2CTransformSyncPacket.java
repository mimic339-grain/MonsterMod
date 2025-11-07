package com.mimic.monstermod.network.server;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー → クライアント
 * Playerの変身状態（Identity）同期用
 * 回転情報は送らず描画前コピーに任せる
 */
public class S2CTransformSyncPacket {

    private final UUID playerId;
    private final CompoundTag nbt;

    public S2CTransformSyncPacket(UUID playerId, CompoundTag nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    /** エンコード */
    public static void encode(S2CTransformSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    /** デコード */
    public static S2CTransformSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CTransformSyncPacket(buf.readUUID(), buf.readNbt());
    }

    /** クライアント側で処理 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player player = mc.level.getPlayerByUUID(playerId);
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(transformation -> {
                        // --- ① NBT反映（変身データ読み込み） ---
                        transformation.deserializeNBT(player, nbt);

                        // --- ② Identity取得 ---
                        BaseMonsterIdentity identity = transformation.getIdentity();
                        if (identity == null) return;

                        try {
                            // --- ③ Entity 確保（なければ生成） ---
                            BaseMonsterEntity entity = identity.getEntity();
                            if (entity == null) {
                                // identity がクライアントで自前 entity を作れるなら作らせる（ensureClientEntity は public）
                                identity.ensureClientEntity(player);
                                entity = identity.getEntity();
                            }

                            // --- ④ モデル初期化（Entity 側で実行） ---
                            if (entity != null) {
                                entity.ensureModelInitialized();
                                // BoneMap が未初期化なら明示的に初期化（冪等）
                                identity.autoInitBoneMap(entity);
                            } else {
                                // entity が確保できなかった場合はログだけ出しておく
                                MonsterMod.LOGGER.warn("[S2CTransformSyncPacket] Could not obtain client entity for identity {}", identity.getId());
                            }
                        } catch (Exception e) {
                            MonsterMod.LOGGER.error("[S2CTransformSyncPacket] Identity initialization failed", e);
                        }
                    });
        });
        ctx.get().setPacketHandled(true);
    }

    /** サーバー側で送信するNBT作成 */
    public static CompoundTag createNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> tag.merge(transformation.serializeNBT()));

        // 回転情報は描画前コピーに任せる
        return tag;
    }
}
