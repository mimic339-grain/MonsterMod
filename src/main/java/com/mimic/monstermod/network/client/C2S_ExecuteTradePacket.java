package com.mimic.monstermod.network.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_OpenTradePacket;
import com.mimic.monstermod.npc.NpcTrade;
import com.mimic.monstermod.npc.NpcTradeSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー。交渉の実行を要求する。
 *
 * 【サーバー権威】所持品の確認・消費・在庫の減算は全てサーバーで行う。
 * クライアントは「何番目の交渉を選んだか」しか送らないため、
 * 画面を改造しても在庫や所持品を偽装できない。
 */
public class C2S_ExecuteTradePacket {

    private final int entityId;
    private final int tradeIndex;

    public C2S_ExecuteTradePacket(int entityId, int tradeIndex) {
        this.entityId = entityId;
        this.tradeIndex = tradeIndex;
    }

    public static void encode(C2S_ExecuteTradePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeVarInt(m.tradeIndex);
    }

    public static C2S_ExecuteTradePacket decode(FriendlyByteBuf buf) {
        return new C2S_ExecuteTradePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(C2S_ExecuteTradePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity npc = player.level().getEntity(msg.entityId);
            if (npc == null) return;

            // 離れた場所から交換されないよう距離を確認する
            if (player.distanceToSqr(npc) > 64.0D) return;

            NpcTradeSet set = NpcTradeSet.load(npc);
            if (msg.tradeIndex < 0 || msg.tradeIndex >= set.getTrades().size()) return;

            NpcTrade trade = set.getTrades().get(msg.tradeIndex);
            if (trade.isSoldOut()) {
                player.displayClientMessage(Component.literal("この交渉はもう受けられません"), true);
                return;
            }
            if (!trade.canAfford(player)) {
                player.displayClientMessage(Component.literal("アイテムが足りません"), true);
                return;
            }

            if (trade.execute(player)) {
                NpcTradeSet.save(npc, set); // 減った在庫を保存する
                ModMessages.sendToPlayer(new S2C_OpenTradePacket(
                        msg.entityId, npc.getName().getString(), set, true), player);
            }
        });
        context.setPacketHandled(true);
    }
}
