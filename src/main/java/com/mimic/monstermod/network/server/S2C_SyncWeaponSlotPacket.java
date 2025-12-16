package com.mimic.monstermod.network.server;

import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント
 * Hunter WeaponSlot 同期パケット
 *
 * ・UI / Menu 非依存
 * ・Capability のみを更新
 * ・Slot 描画は vanilla に完全委譲
 */
public class S2C_SyncWeaponSlotPacket {

    private final ItemStack stack;

    public S2C_SyncWeaponSlotPacket(ItemStack stack) {
        ItemStack s = stack == null ? ItemStack.EMPTY : stack.copy();
        s.setCount(1);
        this.stack = s;
    }

    /* ==============================
     * Encode / Decode
     * ============================== */

    public static void encode(S2C_SyncWeaponSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
    }

    public static S2C_SyncWeaponSlotPacket decode(FriendlyByteBuf buf) {
        return new S2C_SyncWeaponSlotPacket(buf.readItem());
    }

    /* ==============================
     * Handle（Client only）
     * ============================== */

    public static void handle(S2C_SyncWeaponSlotPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            ItemStack incoming = msg.stack;

            // ★ 最終防衛ライン
            ItemStack safe;
            if (incoming.isEmpty()) {
                safe = ItemStack.EMPTY;
            } else if (!(incoming.getItem() instanceof WeaponItem)) {
                safe = ItemStack.EMPTY;
            } else {
                safe = incoming.copy();
                safe.setCount(1);
            }

            mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .ifPresent(cap -> cap.setWeaponSlotClient(safe));
        });

        ctx.setPacketHandled(true);
    }
}
