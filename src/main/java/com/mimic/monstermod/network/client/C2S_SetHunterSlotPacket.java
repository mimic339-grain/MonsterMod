package com.mimic.monstermod.network.client;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_SetHunterSlotPacket {

    private final ItemStack newItem;

    public C2S_SetHunterSlotPacket(ItemStack newItem) {
        this.newItem = newItem.copy();
    }

    public C2S_SetHunterSlotPacket(FriendlyByteBuf buf) {
        this.newItem = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItem(newItem);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

            // Capability を安全に取得
            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
                // Capability のスロットに代入（同期も含む）
                cap.setHunterSlot(newItem, player);
            });
        });
        context.setPacketHandled(true);
    }
}
