package com.mimic.monstermod.network.client;

import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_SetWeaponSlotPacket {
    // 引数なしで統一（サーバー側で「今手に持っているもの」を判定するため）
    public C2S_SetWeaponSlotPacket() {}

    public static void encode(C2S_SetWeaponSlotPacket msg, FriendlyByteBuf buf) {}
    public static C2S_SetWeaponSlotPacket decode(FriendlyByteBuf buf) { return new C2S_SetWeaponSlotPacket(); }

    public static void handle(C2S_SetWeaponSlotPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(ht -> {
                // 1. まず「マウスで掴んでいるアイテム」を確認（インベントリ操作用）
                ItemStack carried = player.containerMenu.getCarried();
                // 2. 次に「メインハンドで持っているアイテム」を確認（右クリック装備用）
                ItemStack hand = player.getMainHandItem();

                ItemStack inSlot = ht.getWeaponSlot().copy();

                // インベントリで操作中の場合
                if (!carried.isEmpty() && carried.getItem() instanceof WeaponItem) {
                    ht.setWeaponSlotServer(carried.copy(), player);
                    player.containerMenu.setCarried(inSlot);
                    ht.syncEquippedFromSlot(player);
                }
                // 手に持った状態で右クリックした場合（マウスが空の時のみ）
                else if (carried.isEmpty() && hand.getItem() instanceof WeaponItem) {
                    ht.setWeaponSlotServer(hand.copy(), player);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, inSlot);
                    ht.syncEquippedFromSlot(player);
                }
                // 何も持たずにクリック（拾う処理）
                else if (carried.isEmpty() && hand.isEmpty() && !inSlot.isEmpty()) {
                    ht.setWeaponSlotServer(ItemStack.EMPTY, player);
                    player.containerMenu.setCarried(inSlot);
                    ht.syncEquippedFromSlot(player);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}