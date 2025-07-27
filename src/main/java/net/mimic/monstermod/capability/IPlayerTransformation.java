package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.entity.custom.MimicEntity;

/**
 * プレイヤーの変身状態を管理するためのインターフェース。
 * プレイヤーごとに異なる変身情報を保持します。
 */
public interface IPlayerTransformation {
    boolean isTransformed();
    void setTransformed(boolean transformed);

    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    // ★変更: Mimic固有のアニメーション状態
    MimicEntity.MimicAnimationState getMimicState();
    void setMimicState(MimicEntity.MimicAnimationState state);

    // ★変更: 噛みつき状態
    boolean isBiting();
    void setBiting(boolean biting);

    // サーバーからクライアントへデータを同期するメソッド
    void syncToClient(Player player);

    // NBTデータとの間でCapabilityのデータをシリアライズ/デシリアライズするメソッド
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}