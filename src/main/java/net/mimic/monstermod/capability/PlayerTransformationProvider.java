package net.mimic.monstermod.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTransformationProvider implements ICapabilityProvider {

    public static final Capability<PlayerTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<PlayerTransformation>() {});

    private PlayerTransformation transformation;
    private final LazyOptional<PlayerTransformation> optional =
            LazyOptional.of(this::createPlayerTransformation);

    private PlayerTransformation createPlayerTransformation() {
        if (transformation == null) transformation = new PlayerTransformation();
        return transformation;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_TRANSFORMATION) return optional.cast();
        return LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        PlayerTransformation pt = createPlayerTransformation();
        tag.putBoolean("transformed", pt.isTransformed());
        if (pt.getTransformedMobId() != null)
            tag.putString("transformedMobId", pt.getTransformedMobId().toString());
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        PlayerTransformation pt = createPlayerTransformation();
        pt.setTransformed(nbt.getBoolean("transformed"));
        if (nbt.contains("transformedMobId"))
            pt.setTransformedMobId(new ResourceLocation(nbt.getString("transformedMobId")));
    }

    public void invalidate() { optional.invalidate(); }
}
