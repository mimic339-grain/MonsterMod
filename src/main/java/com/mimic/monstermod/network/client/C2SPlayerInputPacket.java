package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.impl.ServerInputHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2SPlayerInputPacket 最終版
 * - 攻撃キー削除
 * - スキル / 回避 / メニューを統合
 * - YSMMOD方式準拠
 */
public class C2SPlayerInputPacket {

    private final boolean menuKey;
    private final boolean dodgeKey;
    private final int skillIndex;

    public C2SPlayerInputPacket(boolean menuKey, boolean dodgeKey, int skillIndex) {
        this.menuKey = menuKey;
        this.dodgeKey = dodgeKey;
        this.skillIndex = skillIndex;
    }

    public static void encode(C2SPlayerInputPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.menuKey);
        buf.writeBoolean(pkt.dodgeKey);
        buf.writeInt(pkt.skillIndex);
    }

    public static C2SPlayerInputPacket decode(FriendlyByteBuf buf) {
        return new C2SPlayerInputPacket(buf.readBoolean(), buf.readBoolean(), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // ServerInputHandler に登録
            ServerInputHandler.getInstance().updateInput(player, dodgeKey, skillIndex);

            // メニュー入力は即時反映
            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        if (!trans.isTransformed()) return;
                        BaseMonsterIdentity identity = trans.getIdentity();
                        if (identity != null && menuKey) {
                            trans.handleMenuInput(player);
                        }
                    });
        });
        ctx.get().setPacketHandled(true);
    }
}
