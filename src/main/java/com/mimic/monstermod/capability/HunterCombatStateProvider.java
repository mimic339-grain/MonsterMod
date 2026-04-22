package com.mimic.monstermod.capability;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HunterCombatStateProvider — 完全版
 * - プレイヤーに HunterCombatState をアタッチする Provider
 * - CapabilityRegistry に登録された CAP_HUNTER_COMBAT_STATE と接続する
 * - serializeNBT / deserializeNBT を完全実装
 */
public class HunterCombatStateProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath("monstermod", "hunter_combat_state");

    /** 内部に保持する実データ */
    private final HunterCombatState instance = new HunterCombatState();

    /** LazyOptional（Forge の Capability 管理の基本） */
    private final LazyOptional<HunterCombatState> opt = LazyOptional.of(() -> instance);

    // --------------------------
    // Capability 取得処理
    // --------------------------
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        if (cap == CapabilityRegistry.HUNTER_COMBAT_STATE) {
            return opt.cast();
        }

        return LazyOptional.empty();
    }

    // --------------------------
    // NBT シリアライズ
    // --------------------------
    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }
}
