package com.mimic.monstermod.mixin.accessor;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * AbstractContainerMenu の protected メソッドを
 * Mixin から呼ぶための Accessor
 */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {

    /**
     * protected Slot addSlot(Slot slot)
     * を外部から呼べるようにする
     */
    @Invoker("addSlot")
    Slot callAddSlot(Slot slot);
}
