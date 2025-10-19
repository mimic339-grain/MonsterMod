package com.mimic.monstermod.network.server;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
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
                        // 変身NBTを適用（能力クールタイム・装備など）
                        transformation.deserializeNBT(nbt);

                        // 描画用Identityの取得（renderMixinで描画前にPlayerからコピー）
                        BaseMonsterIdentity identity = transformation.getIdentity(player.getCommandSenderWorld(), player);
                        // 回転はここでは扱わない
                    });
        });
        ctx.get().setPacketHandled(true);
    }

    /** サーバー側で送信するNBT作成 */
    public static CompoundTag createNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> tag.merge(transformation.serializeNBT()));

        // ここでは回転情報は含めない（描画前コピーに任せる）

        return tag;
    }
}
