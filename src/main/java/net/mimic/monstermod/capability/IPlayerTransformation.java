package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public interface IPlayerTransformation {
    boolean isTransformed();
    void setTransformed(boolean transformed);

    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    LivingEntity getOriginalMob();
    void setOriginalMob(LivingEntity mob);

    boolean isMimicOpen();
    void setMimicOpen(boolean isOpen);

    void saveNBTData(CompoundTag nbt);
    void loadNBTData(CompoundTag nbt);
}