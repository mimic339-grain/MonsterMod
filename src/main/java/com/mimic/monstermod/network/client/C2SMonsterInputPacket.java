package com.mimic.monstermod.network.client;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー
 * プレイヤー入力（スキル / メニュー / 回避）を送信
 * Monster / Hunter 両方対応
 */
public class C2SMonsterInputPacket {

    private final boolean useKey;      // スキルキー
    private final boolean menuKey;     // メニューキー
    private final boolean dodgeKey;    // 回避キー
    private final int skillIndex;      // スキル番号

    public C2SMonsterInputPacket(boolean useKey, boolean menuKey, boolean dodgeKey, int skillIndex) {
        this.useKey = useKey;
        this.menuKey = menuKey;
        this.dodgeKey = dodgeKey;
        this.skillIndex = skillIndex;
    }

    // ===== エンコード / デコード =====
    public static void encode(C2SMonsterInputPacket pkt, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.useKey);
        buf.writeBoolean(pkt.menuKey);
        buf.writeBoolean(pkt.dodgeKey);
        buf.writeInt(pkt.skillIndex);
    }

    public static C2SMonsterInputPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new C2SMonsterInputPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt()
        );
    }

    // ===== サーバ側処理 =====
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                System.out.println("[C2SPlayerInputPacket] player is null");
                return;
            }

            // ★ 受信確認ログ ★
            System.out.println("[C2SPlayerInputPacket] Packet received from player: " + player.getName().getString() +
                    " | useKey=" + useKey + " menuKey=" + menuKey + " dodgeKey=" + dodgeKey + " skillIndex=" + skillIndex);

            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        BaseMonsterIdentity identity = trans.getIdentity(); // 変数名 trans に合わせる
                        if (identity == null) {
                            System.out.println("[C2SPlayerInputPacket] identity is null for player: " + player.getName().getString());
                            return;
                        }

                        // メニュー処理
                        if (menuKey) {
                            System.out.println("[C2SPlayerInputPacket] handleMenu called");
                            identity.handleMenu(player);
                        }

                        // スキル処理
                        if (useKey && skillIndex >= 0) {
                            System.out.println("[C2SPlayerInputPacket] handleAbility called for skill " + skillIndex);
                            identity.handleAbility(player, skillIndex);
                        }

                        // 回避処理
                        if (dodgeKey) {
                            System.out.println("[C2SPlayerInputPacket] handleDodge called");
                            identity.handleDodge(player);
                        }
                    });
        });

        ctx.get().setPacketHandled(true);
    }
}
