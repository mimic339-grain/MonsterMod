package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.BlockBombMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * サーバー → クライアント。ブロックに仕掛けられたボムの位置を送る。
 *
 * 【ボマーにしか送らない理由】
 * 仕掛けた場所が全員に見えると仕掛けとして成立しない。
 * 描くときにボマーかどうかを見るだけでは、送られた情報自体は
 * 誰のクライアントにも届いてしまうので、送る相手をサーバー側で絞っている。
 *
 * 数が少ない(せいぜい数個)ので、差分ではなく一覧をそのまま送っている。
 */
public class S2C_BlockBombMarksPacket {

    /** 仕掛けてある場所。ボマーにしか送らない */
    private final List<BlockPos> secret;
    /** 踏まれて起動した場所。証拠として全員に送る */
    private final List<BlockPos> revealed;

    public S2C_BlockBombMarksPacket(List<BlockPos> secret, List<BlockPos> revealed) {
        this.secret = secret;
        this.revealed = revealed;
    }

    public static void encode(S2C_BlockBombMarksPacket msg, FriendlyByteBuf buf) {
        writeList(buf, msg.secret);
        writeList(buf, msg.revealed);
    }

    public static S2C_BlockBombMarksPacket decode(FriendlyByteBuf buf) {
        return new S2C_BlockBombMarksPacket(readList(buf), readList(buf));
    }

    private static void writeList(FriendlyByteBuf buf, List<BlockPos> list) {
        buf.writeVarInt(list.size());
        for (BlockPos pos : list) buf.writeBlockPos(pos);
    }

    private static List<BlockPos> readList(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<BlockPos> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readBlockPos());
        return list;
    }

    public static void handle(S2C_BlockBombMarksPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BlockBombMarks.replaceAll(msg.secret, msg.revealed)));
        context.setPacketHandled(true);
    }
}
