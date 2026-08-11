package com.mimic.monstermod.capability;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// ICapabilitySerializableを実装することで、Forgeの標準プレイヤーセーブ処理から
// serializeNBT/deserializeNBTが自動的に呼ばれるようになる(以前はICapabilityProviderのみで
// 呼び出し経路が手動のPlayerHPEvents頼みだったため、保存タイミング漏れでHPが巻き戻る不具合があった)。
public class MonsterTransformationProvider implements ICapabilitySerializable<CompoundTag> {

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
    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
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