package net.mimic.monstermod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTransformationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<IPlayerTransformation> PLAYER_TRANSFORMATION = CapabilityManager.get(new CapabilityToken<IPlayerTransformation>() {});

    private PlayerTransformation transformation = null;
    private final LazyOptional<IPlayerTransformation> optional = LazyOptional.of(this::createPlayerTransformation);

    private IPlayerTransformation createPlayerTransformation() {
        if (this.transformation == null) {
            this.transformation = new PlayerTransformation();
        }
        return this.transformation;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_TRANSFORMATION) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerTransformation().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerTransformation().loadNBTData(nbt);
    }
}