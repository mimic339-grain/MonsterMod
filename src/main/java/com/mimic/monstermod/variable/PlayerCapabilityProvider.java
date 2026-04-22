package com.mimic.monstermod.variable;

import com.mimic.monstermod.variable.entity.IPlayerData;
import com.mimic.monstermod.variable.entity.PlayerCap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = new ResourceLocation("monstermod", "player_cap");

    private final PlayerCap backend;
    private final LazyOptional<IPlayerData> optional;

    public PlayerCapabilityProvider(Player owner) {
        this.backend = new PlayerCap(owner);
        this.optional = LazyOptional.of(() -> backend);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityRegistry.PLAYER_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }
}