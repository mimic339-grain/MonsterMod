package com.mimic.monstermod.mixin.accessor;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {

    // protected void setSharedFlag(int, boolean) を呼ぶための Invoker
    @Invoker("setSharedFlag")
    void callSetSharedFlag(int index, boolean value);
}
