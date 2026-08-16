package com.mimic.monstermod.network.client;

import com.mimic.monstermod.block.PlacedBombBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー。設置ボムの起爆時間を決める。
 *
 * クライアントは秒数しか送らない。半径の計算も時間の妥当性の確認もサーバーが行うので、
 * 画面をいじっても無茶な設定にはできない。
 */
public class C2S_SetBombTimerPacket {

    /** 受け付ける時間の範囲(秒)。0は即爆。画面の選択肢もこの中に収まっている */
    private static final int MIN_SECONDS = 0;
    private static final int MAX_SECONDS = 600;
    /** 遠くのボムを勝手に設定できないようにする距離 */
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final BlockPos pos;
    private final int seconds;

    public C2S_SetBombTimerPacket(BlockPos pos, int seconds) {
        this.pos = pos;
        this.seconds = seconds;
    }

    public static void encode(C2S_SetBombTimerPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.seconds);
    }

    public static C2S_SetBombTimerPacket decode(FriendlyByteBuf buf) {
        return new C2S_SetBombTimerPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(C2S_SetBombTimerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (msg.seconds < MIN_SECONDS || msg.seconds > MAX_SECONDS) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5)
                    > MAX_DISTANCE_SQR) return;

            if (!(player.level().getBlockEntity(msg.pos) instanceof PlacedBombBlockEntity be)) return;
            if (be.isArmed()) return; // 一度決めたら変更できない

            be.setOwner(player.getUUID());
            be.startTimer(msg.seconds);

            player.level().playSound(null, msg.pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(Component.literal(
                            msg.seconds <= 0 ? "すぐ爆発する" : (msg.seconds + "秒後に爆発する"))
                    .withStyle(ChatFormatting.RED), true);
        });
        context.setPacketHandled(true);
    }
}
