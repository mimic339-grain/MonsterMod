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

/**
 * Hunter 用 Capability Provider
 * - 中身は HunterTransformation（武器装備＆納刀/抜刀管理）
 * - Monster 用の PlayerTransformationProvider と同じ構造で実装
 */
public class HunterTransformationProvider implements ICapabilityProvider {

    // =====================================================================
    // 内部データ本体
    // =====================================================================
    private final HunterTransformation data = new HunterTransformation();

    // Forge 形式の LazyOptional
    private final LazyOptional<HunterTransformation> optional = LazyOptional.of(() -> data);

    // =====================================================================
    // Capability 取得
    // =====================================================================
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> cap,
            @Nullable Direction side
    ) {
        return cap == CapabilityRegistry.HUNTER_TRANSFORMATION
                ? optional.cast()
                : LazyOptional.empty();
    }

    // =====================================================================
    // NBT 保存 / 読み込み
    // （AttachCapabilitiesEvent などから手動で呼ぶ想定）
    // =====================================================================
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);
    }

    // Player 依存の初期化が欲しくなったらここで呼べるようにしておく
    public void onLoad(Player player) {
        // 今の HunterTransformation は Player 依存の復元処理は不要なので何もしない。
        // 将来「ログイン時にLayer更新」とかしたくなったらここに書く。
    }

    // =====================================================================
    // 生データへのアクセス
    // =====================================================================
    public HunterTransformation get() {
        return data;
    }
}
