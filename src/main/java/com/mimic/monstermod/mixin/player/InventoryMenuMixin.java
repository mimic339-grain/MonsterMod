package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.gui.hunter.WeaponContainer;
import com.mimic.monstermod.gui.hunter.WeaponSlot;
import com.mimic.monstermod.mixin.accessor.AbstractContainerMenuAccessor;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void monstermod$addWeaponSlot(
            Inventory inventory,
            boolean active,
            Player player,
            CallbackInfo ci
    ) {
        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(hunter -> {
            WeaponContainer container =
                    new WeaponContainer(player, hunter);

            WeaponSlot slot =
                    new WeaponSlot(container, -28, 8);

            ((AbstractContainerMenuAccessor) this).callAddSlot(slot);
        });
    }
}
