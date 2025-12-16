package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetWeaponSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class WeaponContainer extends SimpleContainer {

    private final Player player;
    private final HunterTransformation hunter;

    public WeaponContainer(Player player, HunterTransformation hunter) {
        super(1);
        this.player = player;
        this.hunter = hunter;
    }

    // 常に Capability を参照
    @Override
    public ItemStack getItem(int index) {
        return hunter.getWeaponSlot();
    }

    // 書き込みは直接 Capability
    @Override
    public void setItem(int index, ItemStack stack) {
        if (!(player instanceof ServerPlayer sp)) return;
        hunter.setWeaponSlotServer(stack, sp);
    }

    // removeItem は Capability と同期する
    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack current = getItem(index);
        if (current.isEmpty()) return ItemStack.EMPTY;

        if (player.level().isClientSide) {
            // 外す要求をサーバーに送信
            ModMessages.sendToServer(new C2S_SetWeaponSlotPacket(ItemStack.EMPTY));
        }

        // 実際にはクライアントは直接持たない
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof WeaponItem;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }
}
