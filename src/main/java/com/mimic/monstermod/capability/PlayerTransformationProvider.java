package com.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
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

    /** NBT保存 */
    public CompoundTag serializeNBT() {
        return transformation.serializeNBT();
    }

    /** NBT復元 */
    public void deserializeNBT(@Nonnull Player player, @Nonnull CompoundTag tag) {
        transformation.deserializeNBT(player, tag);
    }

    /** Capability定義 */
    public static class PlayerTransformationCapability {
        public static final Capability<PlayerTransformation> PLAYER_TRANSFORMATION =
                CapabilityManager.get(new CapabilityToken<PlayerTransformation>() {});
    }

    /** Getter */
    @Nonnull
    public PlayerTransformation get() {
        return transformation;
    }

    /** Getter（LazyOptional） */
    @Nonnull
    public LazyOptional<PlayerTransformation> getOptional() {
        return optional;
    }
}
