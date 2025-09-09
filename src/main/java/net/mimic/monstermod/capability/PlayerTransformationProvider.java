package net.mimic.monstermod.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlayerTransformation Capabilityをエンティティに提供するためのプロバイダー。
 * 複数Monsterに対応し、ライフサイクル管理とNBT同期を考慮。
 */
public class PlayerTransformationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    // Capabilityの登録
    public static final Capability<IPlayerTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<IPlayerTransformation>() {});

    private IPlayerTransformation transformation = null;

    // LazyOptionalを使って遅延初期化
    private final LazyOptional<IPlayerTransformation> optional = LazyOptional.of(this::createPlayerTransformation);

    private IPlayerTransformation createPlayerTransformation() {
        if (transformation == null) {
            transformation = new PlayerTransformation();
        }
        return transformation;
    }

    /**
     * 他クラスから Capability が要求された場合に返す
     */
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_TRANSFORMATION) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }


    @Override
    public CompoundTag serializeNBT() {
        return createPlayerTransformation().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerTransformation().deserializeNBT(nbt);
    }

    /**
     * ライフサイクル管理用: Capability を無効化する
     * プレイヤーが離脱したときに呼ぶとメモリリーク防止になる
     */
    public void invalidate() {
        optional.invalidate();
    }
}
