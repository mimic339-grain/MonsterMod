package com.mimic.monstermod.weapon;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.item.weapon.WeaponItem;
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

        // 【修正】this.items.set ではなく super.setItem を使う
        // これで private フィールドにアクセスせずに初期値を同期できます
        ItemStack currentWeapon = hunter.getWeaponSlot().copy();
        if (!currentWeapon.isEmpty()) {
            super.setItem(0, currentWeapon);
        }
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);

        // サーバー側でのみ Capability に反映
        if (player instanceof ServerPlayer sp) {
            hunter.setWeaponSlotServer(stack, sp);
            hunter.syncEquippedFromSlot(sp);
        }
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack result = super.removeItem(index, count);
        if (player instanceof ServerPlayer sp) {
            // getItem(index) で現在の（減った後の）アイテムを取得して同期
            hunter.setWeaponSlotServer(getItem(index), sp);
            hunter.syncEquippedFromSlot(sp);
        }
        return result;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof WeaponItem;
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }
}