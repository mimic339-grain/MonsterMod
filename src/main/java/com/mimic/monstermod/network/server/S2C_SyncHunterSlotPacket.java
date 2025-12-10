package com.mimic.monstermod.network.server;

import com.mimic.monstermod.gui.hunter.HunterMeny;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバーからクライアントに送るパケット
 * HunterのWeaponSlot状態を同期する
 */
public class S2C_SyncHunterSlotPacket {

    private final WeaponSlotData data;

    /** 内部クラスで ItemStack を保持（ItemStack だけを送る） */
    public static class WeaponSlotData {
        public final net.minecraft.world.item.ItemStack stack;

        public WeaponSlotData(net.minecraft.world.item.ItemStack stack) {
            this.stack = stack.copy();
        }
    }

    /** コンストラクタ：ItemStack から作成 */
    public S2C_SyncHunterSlotPacket(net.minecraft.world.item.ItemStack stack) {
        this.data = new WeaponSlotData(stack);
    }

    /** コンストラクタ：ネットワークバッファから読み込み */
    public S2C_SyncHunterSlotPacket(FriendlyByteBuf buf) {
        this.data = new WeaponSlotData(buf.readItem());
    }

    /** 書き込み */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItem(data.stack);
    }

    /** パケット処理 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // GUI側の統一メソッドで更新
            HunterMeny.onServerSync(data.stack);
        });
        context.setPacketHandled(true);
    }
}
