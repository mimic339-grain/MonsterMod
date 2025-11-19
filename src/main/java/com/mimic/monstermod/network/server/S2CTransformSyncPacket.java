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

/**
 * サーバー → クライアント
 * Playerの変身状態（Identity / HP）同期用（属性は *サーバーだけ* が authoritative）
 *
 * クライアント側では：
 *  - 変身状態の反映
 *  - playerHP / identityHP の同期
 *  - DummyEntity の生成（描画用）だけ行う
 * 属性のコピーや health の変更は行わない。サーバーが全部管理する。
 */
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

    /** クライアントでの処理 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player player = mc.level.getPlayerByUUID(playerId);
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                    .ifPresent(transformation -> {

                        // 変身状態（isTransformed / mobId）の反映
                        transformation.deserializeNBT(nbt);

                        // === HP同期（Mapに入れるだけ） ===
                        if (nbt.contains("playerHP")) {
                            MonsterTransformUtil.setPlayerHP(player, nbt.getDouble("playerHP"));
                        }

                        BaseMonsterIdentity identity = transformation.getIdentity();
                        if (identity != null && nbt.contains("identityHP")) {
                            MonsterTransformUtil.setIdentityHP(player, identity.getId(), nbt.getDouble("identityHP"));
                        }
                    });
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * サーバー → クライアントへ送る NBT を作成
     */
    public static CompoundTag createNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    // isTransformed / mobId の書き込み
                    tag.merge(transformation.serializeNBT());

                    // HP同期
                    double playerHP = MonsterTransformUtil.getPlayerHP(player);
                    tag.putDouble("playerHP", playerHP);

                    BaseMonsterIdentity identity = transformation.getIdentity();
                    if (identity != null) {
                        double identityHP = MonsterTransformUtil.getIdentityHP(player, identity.getId());
                        tag.putDouble("identityHP", identityHP);
                    }
                });

        return tag;
    }
}
