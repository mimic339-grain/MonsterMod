package com.mimic.monstermod.weapon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WeaponSlot extends Slot {
    private final WeaponContainer weaponContainer;
    private final int index;

    public WeaponSlot(WeaponContainer container, int index, int x, int y) {
        super(container, index, x, y);
        this.weaponContainer = container;
        this.index = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // Container側のロジックに任せる（武器アイテムかどうか等）
        return this.container.canPlaceItem(this.index, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return true; // インベントリの状態に関わらず常に操作可能
    }

    @Override
    public boolean mayPickup(Player player) {
        return !getItem().isEmpty();
    }
}