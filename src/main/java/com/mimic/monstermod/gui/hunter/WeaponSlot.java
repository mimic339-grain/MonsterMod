package com.mimic.monstermod.gui.hunter;

import com.mimic.monstermod.item.weapon.WeaponItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WeaponSlot extends Slot {

    private final WeaponContainer container;

    public WeaponSlot(WeaponContainer container, int x, int y) {
        super(container, 0, x, y);
        this.container = container;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof WeaponItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * InventoryMenu(active=false) でも操作可能にする
     */
    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public ItemStack getItem() {
        return container.getItem(0); // 常に Capability を参照
    }

    @Override
    public boolean mayPickup(Player player) {
        return !getItem().isEmpty();
    }
}
