package com.mimic.monstermod.variable;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.variable.entity.IMonsterData;
import com.mimic.monstermod.variable.entity.MonsterData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class MonsterCapabilityProvider implements ICapabilityProvider {

    public static final ResourceLocation ID = new ResourceLocation(MonsterMod.MOD_ID, "monster_cap");

    private final MonsterData backend;
    private final LazyOptional<IMonsterData> optional;

    public MonsterCapabilityProvider(Entity monster) {
        this.backend = new MonsterData(monster);
        this.optional = LazyOptional.of(() -> backend);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CapabilityRegistry.MONSTER_CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }
}
