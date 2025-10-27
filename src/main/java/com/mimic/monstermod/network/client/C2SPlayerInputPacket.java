package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー
 * プレイヤー入力（スキル / メニュー / 使用キー）を送信
 */
public class C2SPlayerInputPacket {

    private final boolean useKey;      // 使用キー（右クリックなど）
    private final boolean menuKey;     // メニューキー
    private final int skillIndex;      // スキル番号

    public C2SPlayerInputPacket(boolean useKey, boolean menuKey, int skillIndex) {
        this.useKey = useKey;
        this.menuKey = menuKey;
        this.skillIndex = skillIndex;
    }

    public static void encode(C2SPlayerInputPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.useKey);
        buf.writeBoolean(pkt.menuKey);
        buf.writeInt(pkt.skillIndex);
    }

    public static C2SPlayerInputPacket decode(FriendlyByteBuf buf) {
        return new C2SPlayerInputPacket(buf.readBoolean(), buf.readBoolean(), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        if (!trans.isTransformed()) return;
                        BaseMonsterIdentity identity = trans.getIdentity();
                        if (identity == null) return;

                        // Identity 側で入力処理を行う（クールタイムもここでチェック）
                        identity.handleClientInput(player, useKey, menuKey, skillIndex);
                    });
        });
        ctx.get().setPacketHandled(true);
    }
}
