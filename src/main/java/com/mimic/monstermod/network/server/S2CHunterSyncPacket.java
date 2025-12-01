package com.mimic.monstermod.network.server;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー → クライアント
 * Hunter状態の同期（サーバー authoritative）
 *
 * クライアント側：
 *  - HunterTransformation の反映
 *  - 納刀状態（sheath = true/false）の同期
 *  - Layer の切り替え（武器を背中に回す・手に持つ の切替）
 */
public class S2CHunterSyncPacket {

    private final UUID playerId;
    private final CompoundTag nbt;

    public S2CHunterSyncPacket(UUID playerId, CompoundTag nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    public static void encode(S2CHunterSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    public static S2CHunterSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CHunterSyncPacket(buf.readUUID(), buf.readNbt());
    }

    /** クライアントでの処理 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player player = mc.level.getPlayerByUUID(playerId);
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .ifPresent((HunterTransformation hunter) -> {

                        boolean prevSheath = hunter.isSheathed();

                        // NBT反映
                        hunter.deserializeNBT(nbt);

                        // 納刀状態が変化 → Layer 更新
                        boolean nowSheath = hunter.isSheathed();
                        if (prevSheath != nowSheath) {
                            // TODO: Layer表示切替用メソッド
                            // 例: HunterUtil.updateWeaponLayer(player, nowSheath);
                        }
                    });
        });
        ctx.get().setPacketHandled(true);
    }

    /** サーバー → クライアント用 NBT を作成 */
    public static CompoundTag createNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .ifPresent((HunterTransformation hunter) -> {
                    // HunterTransformation の全状態をマージ
                    tag.merge(hunter.serializeNBT());

                    // 納刀フラグ（Layer制御用）
                    tag.putBoolean("sheath", hunter.isSheathed());

                    // HUDやゲージ同期が必要ならここで追加
                    // tag.putFloat("hunterGauge", HunterUtil.getHunterGauge(player));
                });

        return tag;
    }
}
