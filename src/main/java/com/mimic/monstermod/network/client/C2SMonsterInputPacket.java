package com.mimic.monstermod.network.client;

import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー
 * プレイヤー入力（スキル / メニュー）を送信
 */
public class C2SMonsterInputPacket {

    private final boolean useKey;
    private final boolean menuKey;
    private final int skillIndex;

    // コンストラクタから dodgeKey を削除
    public C2SMonsterInputPacket(boolean useKey, boolean menuKey, int skillIndex) {
        this.useKey = useKey;
        this.menuKey = menuKey;
        this.skillIndex = skillIndex;
    }

    // ===== エンコード (書き込み) =====
    public static void encode(C2SMonsterInputPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.useKey);
        buf.writeBoolean(pkt.menuKey);
        buf.writeInt(pkt.skillIndex);
    }

    // ===== デコード (読み込み) =====
    public static C2SMonsterInputPacket decode(FriendlyByteBuf buf) {
        // 書き込んだ順番・個数と完全に一致させる
        return new C2SMonsterInputPacket(
                buf.readBoolean(), // useKey
                buf.readBoolean(), // menuKey
                buf.readInt()      // skillIndex
        );
    }

    // ===== サーバー側処理 =====
    public static void handle(C2SMonsterInputPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                BaseIdentity identity = trans.getIdentity();
                if (identity == null) return;

                // 1. メニュー処理
                if (msg.menuKey) {
                    identity.handleMenu(player);
                }

                // 2. スキル処理 (回避も handleAbility 内で判定される)
                if (msg.useKey && msg.skillIndex >= 0) {
                    identity.handleAbility(player, msg.skillIndex);
                }
            });
        });
        context.setPacketHandled(true);
    }
}