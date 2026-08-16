package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.BombTimerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバー → クライアント。設置ボムの起爆時間を決める画面を開く */
public class S2C_OpenBombTimerPacket {

    private final BlockPos pos;

    public S2C_OpenBombTimerPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(S2C_OpenBombTimerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static S2C_OpenBombTimerPacket decode(FriendlyByteBuf buf) {
        return new S2C_OpenBombTimerPacket(buf.readBlockPos());
    }

    public static void handle(S2C_OpenBombTimerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new BombTimerScreen(msg.pos))));
        context.setPacketHandled(true);
    }
}
