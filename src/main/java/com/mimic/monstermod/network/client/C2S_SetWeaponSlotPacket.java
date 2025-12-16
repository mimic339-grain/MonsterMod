package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SyncWeaponSlotPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_SetWeaponSlotPacket {

    private final ItemStack requested;

    public C2S_SetWeaponSlotPacket(ItemStack requested) {
        this.requested = requested == null ? ItemStack.EMPTY : requested.copy();
        this.requested.setCount(1);
    }

    public static void encode(C2S_SetWeaponSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.requested);
    }

    public static C2S_SetWeaponSlotPacket decode(FriendlyByteBuf buf) {
        return new C2S_SetWeaponSlotPacket(buf.readItem());
    }

    public static void handle(C2S_SetWeaponSlotPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            HunterTransformation ht = player
                    .getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .orElse(null);
            if (ht == null) return;

            ItemStack current = ht.getWeaponSlot().copy();
            ItemStack requested = msg.requested.copy();

            // ① 外す要求（空スタック）
            if (requested.isEmpty()) {
                if (!current.isEmpty()) {
                    player.getInventory().add(current.copy());
                }
                ht.setWeaponSlotServer(ItemStack.EMPTY, player);
                ht.syncEquippedFromSlot(player);
                ModMessages.sendToPlayer(new S2C_SyncWeaponSlotPacket(ItemStack.EMPTY), player);
                return;
            }

            // ② WeaponItem 判定
            if (!(requested.getItem() instanceof WeaponItem)) {
                ModMessages.sendToPlayer(new S2C_SyncWeaponSlotPacket(current), player);
                return;
            }

            // ③ Carried → Inventory から 1 個消費
            boolean removed = false;
            AbstractContainerMenu menu = player.containerMenu;

            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty() && ItemStack.isSameItemSameTags(carried, requested)) {
                carried.shrink(1);
                removed = true;
            }

            if (!removed) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack inv = player.getInventory().getItem(i);
                    if (!inv.isEmpty() && ItemStack.isSameItemSameTags(inv, requested)) {
                        inv.shrink(1);
                        removed = true;
                        break;
                    }
                }
            }

            if (!removed) {
                ModMessages.sendToPlayer(new S2C_SyncWeaponSlotPacket(current), player);
                return;
            }

            // ④ 古い WeaponSlot を返却
            if (!current.isEmpty()) {
                player.getInventory().add(current.copy());
            }

            // ⑤ 新 WeaponSlot 設定
            ItemStack newStack = requested.copy();
            newStack.setCount(1);

            ht.setWeaponSlotServer(newStack, player);
            ht.syncEquippedFromSlot(player);
            ModMessages.sendToPlayer(new S2C_SyncWeaponSlotPacket(newStack), player);

        });
        ctx.setPacketHandled(true);
    }
}
