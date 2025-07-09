package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

public interface IPlayerTransformation extends INBTSerializable<CompoundTag> {
    boolean isTransformed();
    void setTransformed(boolean transformed);
    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);
}