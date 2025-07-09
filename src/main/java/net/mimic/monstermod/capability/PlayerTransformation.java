package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class PlayerTransformation implements IPlayerTransformation {
    private boolean isTransformed = false;
    private ResourceLocation transformedMobId = null;
    private LivingEntity originalMob = null;

    private boolean isMimicOpen = false;

    @Override
    public boolean isTransformed() {
        return isTransformed;
    }

    @Override
    public void setTransformed(boolean transformed) {
        this.isTransformed = transformed;
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
    public LivingEntity getOriginalMob() {
        return originalMob;
    }

    @Override
    public void setOriginalMob(LivingEntity mob) {
        this.originalMob = mob;
    }

    @Override
    public boolean isMimicOpen() {
        return isMimicOpen;
    }

    @Override
    public void setMimicOpen(boolean isOpen) {
        this.isMimicOpen = isOpen;
    }

    @Override
    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) {
            nbt.putString("transformedMobId", transformedMobId.toString());
        }
        nbt.putBoolean("isMimicOpen", isMimicOpen);
    }

    @Override
    public void loadNBTData(CompoundTag nbt) {
        isTransformed = nbt.getBoolean("isTransformed");
        if (nbt.contains("transformedMobId")) {
            transformedMobId = new ResourceLocation(nbt.getString("transformedMobId"));
        } else {
            transformedMobId = null;
        }
        if (nbt.contains("isMimicOpen")) {
            isMimicOpen = nbt.getBoolean("isMimicOpen");
        } else {
            isMimicOpen = false;
        }
    }
}