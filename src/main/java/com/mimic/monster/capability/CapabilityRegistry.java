package com.mimic.monster.capability;

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

public class CapabilityRegistry {

    //Capability識別子
    public static final Capability<TransformCapability> TRANSFORM =
            CapabilityManager.get(new CapabilityToken<>() {});

    //Capabilityの取得・NBT保存・読み込みを担う
    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

        private final TransformCapability instance = new TransformCapability();

        //LazyOptionalはキャッシュして使い回す
        private final LazyOptional<TransformCapability> optional = LazyOptional.of(() -> instance);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == TRANSFORM ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            //クラッシュ回避
            try {
                instance.saveNBTData(tag);
            } catch (Exception e) {
                System.err.println("[CapabilityRegistry.Provider] saveNBTData failed: " + e.getMessage());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            try {
                instance.loadNBTData(nbt);
            } catch (Exception e) {
                System.err.println("[CapabilityRegistry.Provider] loadNBTData failed: " + e.getMessage());
            }
        }
        //Capabilityが不要になった場合に呼ぶ
        public void invalidate() {
            optional.invalidate();
        }
    }
}
