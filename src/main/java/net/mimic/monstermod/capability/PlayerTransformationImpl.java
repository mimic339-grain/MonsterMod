package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

public class PlayerTransformationImpl implements IPlayerTransformation {
    private boolean transformed = false;
    private ResourceLocation transformedMobId = null;

    @Override
    public boolean isTransformed() {
        return transformed;
    }

    @Override
    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    @Override
    public ResourceLocation getTransformedMobId() {
        return transformedMobId;
    }

    @Override
    public void setTransformedMobId(ResourceLocation mobId) {
        this.transformedMobId = mobId;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("transformed", this.transformed);
        if (this.transformedMobId != null) {
            nbt.putString("transformedMobId", this.transformedMobId.toString());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.transformed = nbt.getBoolean("transformed");
        if (nbt.contains("transformedMobId")) {
            this.transformedMobId = new ResourceLocation(nbt.getString("transformedMobId"));
        }
    }
}