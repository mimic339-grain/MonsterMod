package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag; // NBTのために追加
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity; // LivingEntityのために追加
import net.mimic.monstermod.entity.custom.MimicEntity; // MimicAnimationStateのために追加

public interface IPlayerTransformation {
    boolean isTransformed();
    void setTransformed(boolean transformed);

    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    // ★追加: LivingEntityの参照を保持する（昔のコードから）
    LivingEntity getOriginalMob();
    void setOriginalMob(LivingEntity mob);

    // Mimic固有の状態管理（今のコードから）
    MimicEntity.MimicAnimationState getMimicState();
    void setMimicState(MimicEntity.MimicAnimationState state);

    boolean isBiting();
    void setBiting(boolean biting);

    // ★追加: NBTの保存・ロードメソッド（昔のコードから）
    void saveNBTData(CompoundTag nbt);
    void loadNBTData(CompoundTag nbt);

    // Capability間でデータをコピーするメソッド（今のコードから）
    void copyFrom(IPlayerTransformation source);
}