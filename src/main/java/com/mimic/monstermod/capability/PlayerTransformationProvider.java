package com.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerTransformationProvider implements ICapabilityProvider {

    // Capability 定義（外部から直接参照可能）
    public static final Capability<PlayerTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<PlayerTransformation>(){});

    private final PlayerTransformation transformation = new PlayerTransformation();
    private final LazyOptional<PlayerTransformation> optional = LazyOptional.of(() -> transformation);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_TRANSFORMATION) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    // NBT同期
    public CompoundTag serializeNBT() {
        return transformation.serializeNBT();
    }

    public void deserializeNBT(CompoundTag tag) {
        transformation.deserializeNBT(tag);
    }

    // Getter
    public PlayerTransformation get() {
        return transformation;
    }
}
