package com.mimic.monstermod.network.server;

import com.mimic.monstermod.boss.BossBarStyle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー → クライアント。ボスバー1本ぶんの「枠デザイン」を伝える。
 *
 * 【なぜ別のパケットが必要か】
 * バニラのボスバーは色(6色)と分割の形しか送れず、独自のテクスチャを指定できない。
 * そこでHPと表示先はバニラの {@link net.minecraft.server.level.ServerBossEvent} に任せたまま、
 * 枠のデザインだけをこのパケットで別送りしている。
 *
 * バーのUUIDを鍵にしてクライアント側の表に入れておき、
 * {@link com.mimic.monstermod.client.BossBarRenderer} が描くときに引く。
 * この表に無いバーは他MODやバニラのボスなので、何もせずバニラの描画に任せる。
 *
 * 送信元: {@link com.mimic.monstermod.boss.MonsterBossBars#refreshViewers}
 */
public class S2C_BossBarStylePacket {

    /** バニラのボスバーが持っているUUID。これで対応付ける */
    private final UUID barId;
    /** {@link BossBarStyle} の並び順。知らない番号が来ても既定値に倒す */
    private final int styleId;

    public S2C_BossBarStylePacket(UUID barId, int styleId) {
        this.barId = barId;
        this.styleId = styleId;
    }

    public static void encode(S2C_BossBarStylePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.barId);
        buf.writeVarInt(msg.styleId);
    }

    public static S2C_BossBarStylePacket decode(FriendlyByteBuf buf) {
        return new S2C_BossBarStylePacket(buf.readUUID(), buf.readVarInt());
    }

    public static void handle(S2C_BossBarStylePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.mimic.monstermod.client.BossBarRenderer.setStyle(
                        msg.barId, BossBarStyle.byId(msg.styleId))));
        context.setPacketHandled(true);
    }
}
