package com.mimic.monstermod.network.client;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SyncHunterSlotPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアントから「Hunterスロットを更新／GUI初期化要求」を送るパケット
 */
public class C2S_SetHunterSlotPacket {

    private final boolean requestOnly; // GUI初期化用フラグ
    private final ItemStack stack;     // 更新用アイテム

    /** 更新用コンストラクタ */
    public C2S_SetHunterSlotPacket(ItemStack stack) {
        this.stack = stack.copy();
        this.requestOnly = false;
    }

    /** GUI初期化用 */
    public static C2S_SetHunterSlotPacket createRequest() {
        return new C2S_SetHunterSlotPacket(true);
    }

    private C2S_SetHunterSlotPacket(boolean requestOnly) {
        this.stack = ItemStack.EMPTY;
        this.requestOnly = requestOnly;
    }

    /** ネットワーク読み込み */
    public C2S_SetHunterSlotPacket(FriendlyByteBuf buf) {
        this.requestOnly = buf.readBoolean();
        this.stack = buf.readItem();
    }

    /** 書き込み */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(requestOnly);
        buf.writeItem(stack);
    }

    /** パケット処理（サーバー側） */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {

                if (requestOnly) {
                    // GUI 初期化要求：現在の WeaponSlot をクライアントに送信
                    ModMessages.sendToPlayer(new S2C_SyncHunterSlotPacket(cap.getWeaponSlot()), player);
                } else {
                    // WeaponSlot 更新要求
                    if (stack.isEmpty() || !(stack.getItem() instanceof com.mimic.monstermod.item.weapon.WeaponItem))
                        return;

                    // 既に装備している場合はインベントリに戻す
                    ItemStack old = cap.getWeaponSlot().copy();
                    if (!old.isEmpty()) {
                        if (!player.getInventory().add(old)) {
                            // インベントリに入らなければドロップ
                            player.drop(old, false);
                        }
                    }

                    // WeaponSlot に新しいアイテムをセット
                    cap.setWeaponSlot(stack.copy(), player);

                    // クライアントに同期
                    ModMessages.sendToPlayer(new S2C_SyncHunterSlotPacket(cap.getWeaponSlot()), player);
                }
            });
        });
        context.setPacketHandled(true);
    }
}
