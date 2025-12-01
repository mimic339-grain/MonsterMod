package com.mimic.monstermod.capability;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MonsterTransformationProvider implements ICapabilityProvider {

    // Capability 内部データの実体
    private final MonsterTransformation data = new MonsterTransformation();

    // LazyOptional（Forge 方式）
    private final LazyOptional<MonsterTransformation> optional = LazyOptional.of(() -> data);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> cap,
            @Nullable Direction side
    ) {
        return cap == CapabilityRegistry.PLAYER_TRANSFORMATION
                ? optional.cast()
                : LazyOptional.empty();
    }

    // ======= NBT 保存 / 読み込み =======
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);  // ※Player は必要ない。内部で必要なら別途渡す
    }
    public void onLoad(Player player) {
        data.onLoad(player); // プレイヤー依存の初期化を別メソッドで呼ぶ
    }
    // データへの getter
    public MonsterTransformation get() {
        return data;
    }
}