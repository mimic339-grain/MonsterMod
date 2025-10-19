package com.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

public class PlayerTransformationProvider implements ICapabilityProvider {

    private final PlayerTransformation transformation = new PlayerTransformation();
    private final LazyOptional<PlayerTransformation> optional = LazyOptional.of(() -> transformation);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, net.minecraft.core.Direction side) {
        if (cap == PlayerTransformationCapability.PLAYER_TRANSFORMATION) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        return transformation.serializeNBT();
    }

    public void deserializeNBT(CompoundTag tag) {
        transformation.deserializeNBT(tag);
    }

    public static class PlayerTransformationCapability {
        public static final Capability<PlayerTransformation> PLAYER_TRANSFORMATION =
                CapabilityManager.get(new CapabilityToken<PlayerTransformation>() {});
    }

    public PlayerTransformation get() {
        return transformation;
    }
}
