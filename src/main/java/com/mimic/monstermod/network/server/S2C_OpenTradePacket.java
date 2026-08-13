package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.NpcTradeScreen;
import com.mimic.monstermod.npc.NpcTradeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント。交渉画面を開く/開いている画面の在庫を更新する。
 *
 * 交渉内容はサーバー側(エンティティのpersistentData)が正なので、
 * 開くときも交換成立後も、サーバーから現在の内容を丸ごと送って表示を合わせる。
 */
public class S2C_OpenTradePacket {

    private final int entityId;
    private final String npcName;
    private final NpcTradeSet trades;
    private final boolean refreshOnly; // trueなら既に開いている画面の更新だけ行う

    public S2C_OpenTradePacket(int entityId, String npcName, NpcTradeSet trades, boolean refreshOnly) {
        this.entityId = entityId;
        this.npcName = npcName;
        this.trades = trades;
        this.refreshOnly = refreshOnly;
    }

    public static void encode(S2C_OpenTradePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeUtf(m.npcName);
        m.trades.write(buf);
        buf.writeBoolean(m.refreshOnly);
    }

    public static S2C_OpenTradePacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String name = buf.readUtf();
        NpcTradeSet set = NpcTradeSet.read(buf);
        return new S2C_OpenTradePacket(id, name, set, buf.readBoolean());
    }

    public static void handle(S2C_OpenTradePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> open(msg)));
        context.setPacketHandled(true);
    }

    private static void open(S2C_OpenTradePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (msg.refreshOnly && mc.screen instanceof NpcTradeScreen screen
                && screen.getNpcEntityId() == msg.entityId) {
            screen.updateTrades(msg.trades); // 交換成立後の在庫更新
            return;
        }
        mc.setScreen(new NpcTradeScreen(msg.entityId, msg.npcName, msg.trades));
    }
}
