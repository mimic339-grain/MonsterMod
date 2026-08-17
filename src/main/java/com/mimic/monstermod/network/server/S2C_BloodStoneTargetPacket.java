package com.mimic.monstermod.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント。血石が追っている相手の今の座標を送る。
 *
 * 【石を持っている本人にしか送らない】
 * 追跡先の座標は、他人に知られてよい情報ではない。
 * 描く側で絞るだけではデータ自体が全員に届いてしまうので、送信の段階で相手を限定している。
 * (ボマーの仕掛けを本人にしか送らないのと同じ考え方)
 *
 * 【座標をアイテムのNBTに持たせない理由】
 * 座標は常に最新でなければ意味がない。NBTに書くと同期のたびに
 * インベントリ全体が飛ぶうえ、石を持っていない間も更新し続けることになる。
 * 持っている間だけこのパケットを流すほうが軽く、情報も漏れない。
 *
 * 送信元: {@link com.mimic.monstermod.item.BloodStoneEvents}
 * 受け取り: {@link com.mimic.monstermod.client.BloodStoneCompass}
 */
public class S2C_BloodStoneTargetPacket {

    /** 相手が今サーバーにいるか。いなければ座標は意味を持たない */
    private final boolean online;
    /** 同じディメンションにいるか。違えば方角を指しても無意味なので分けている */
    private final boolean sameDimension;
    private final double x, y, z;

    public S2C_BloodStoneTargetPacket(boolean online, boolean sameDimension,
                                      double x, double y, double z) {
        this.online = online;
        this.sameDimension = sameDimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(S2C_BloodStoneTargetPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.online);
        buf.writeBoolean(msg.sameDimension);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static S2C_BloodStoneTargetPacket decode(FriendlyByteBuf buf) {
        return new S2C_BloodStoneTargetPacket(
                buf.readBoolean(), buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(S2C_BloodStoneTargetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.mimic.monstermod.client.BloodStoneCompass.receive(
                        msg.online, msg.sameDimension, msg.x, msg.y, msg.z)));
        context.setPacketHandled(true);
    }
}
