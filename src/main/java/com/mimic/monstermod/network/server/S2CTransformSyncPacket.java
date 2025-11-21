package com.mimic.monstermod.network.server;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.util.MonsterTransformUtil;
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

        System.out.println("[TransformSync] Packet created for " + playerId);
    }

    public static void encode(S2CTransformSyncPacket msg, FriendlyByteBuf buf) {
        System.out.println("[TransformSync] encode(): player=" + msg.playerId);
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    public static S2CTransformSyncPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        CompoundTag tag = buf.readNbt();
        System.out.println("[TransformSync] decode(): player=" + id);
        return new S2CTransformSyncPacket(id, tag);
    }

    /** クライアントでの処理 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        System.out.println("[TransformSync] handle(): received packet for " + playerId);

        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                System.out.println("[TransformSync] handle(): mc.level is null");
                return;
            }

            Player player = mc.level.getPlayerByUUID(playerId);
            if (player == null) {
                System.out.println("[TransformSync] handle(): target player not found");
                return;
            }

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                    .ifPresent(transformation -> {

                        System.out.println("[TransformSync] Applying transform state to player=" + player.getName().getString());

                        // --- NBT反映（変身状態） ---
                        transformation.deserializeNBT(nbt);
                        System.out.println("[TransformSync] deserializeNBT applied");

                        // --- HP同期 ---
                        if (nbt.contains("playerHP")) {
                            double hp = nbt.getDouble("playerHP");
                            MonsterTransformUtil.setPlayerHP(player, hp);
                            System.out.println("[TransformSync] playerHP synced: " + hp);
                        }

                        BaseMonsterIdentity identity = transformation.getIdentity();
                        if (identity != null && nbt.contains("identityHP")) {
                            double idHP = nbt.getDouble("identityHP");
                            MonsterTransformUtil.setIdentityHP(player, identity.getId(), idHP);
                            System.out.println("[TransformSync] identityHP synced: " + idHP + " (id=" + identity.getId() + ")");
                        }
                    });
        });

        ctx.get().setPacketHandled(true);
    }

    /** サーバー → クライアントへ送る NBT を作成 */
    public static CompoundTag createNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        System.out.println("[TransformSync] createNBT(): building NBT for " + player.getName().getString());

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {

                    // 変身状態を書き込み
                    tag.merge(transformation.serializeNBT());
                    System.out.println("[TransformSync] write: isTransformed=" + transformation.isTransformed()
                            + ", mobId=" + transformation.getMobId());

                    // HP同期
                    double playerHP = MonsterTransformUtil.getPlayerHP(player);
                    tag.putDouble("playerHP", playerHP);
                    System.out.println("[TransformSync] write: playerHP=" + playerHP);

                    BaseMonsterIdentity identity = transformation.getIdentity();
                    if (identity != null) {
                        double idHP = MonsterTransformUtil.getIdentityHP(player, identity.getId());
                        tag.putDouble("identityHP", idHP);
                        System.out.println("[TransformSync] write: identityHP=" + idHP + " (id=" + identity.getId() + ")");
                    }
                });

        return tag;
    }
}
