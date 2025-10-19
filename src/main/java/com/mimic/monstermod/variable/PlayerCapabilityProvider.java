package com.mimic.monstermod.variable;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.variable.entity.IPlayerData;
import com.mimic.monstermod.variable.entity.PlayerCap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerCapabilityProvider implements ICapabilityProvider {

    public static final ResourceLocation ID = new ResourceLocation(MonsterMod.MOD_ID, "player_cap");

    private final PlayerCap backend;
    private final LazyOptional<IPlayerData> optional;

    public PlayerCapabilityProvider(Player owner) {
        this.backend = new PlayerCap(owner);
        this.optional = LazyOptional.of(() -> backend);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == null) return LazyOptional.empty();
        return cap == CapabilityRegistry.PLAYER_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }

    /** 安全に破棄 */
    public void invalidate() {
        optional.invalidate();
    }
}
