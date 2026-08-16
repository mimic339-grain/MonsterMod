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

    private final List<BlockPos> positions;

    public S2C_BlockBombMarksPacket(List<BlockPos> positions) {
        this.positions = positions;
    }

    public static void encode(S2C_BlockBombMarksPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.positions.size());
        for (BlockPos pos : msg.positions) buf.writeBlockPos(pos);
    }

    public static S2C_BlockBombMarksPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<BlockPos> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readBlockPos());
        return new S2C_BlockBombMarksPacket(list);
    }

    public static void handle(S2C_BlockBombMarksPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                BlockBombMarks.replaceAll(msg.positions)));
        context.setPacketHandled(true);
    }
}
