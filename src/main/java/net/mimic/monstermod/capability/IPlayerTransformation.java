package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer; // ServerPlayerをインポート
import net.mimic.monstermod.entity.custom.MimicEntity;

public interface IPlayerTransformation {
    boolean isTransformed();
    void setTransformed(boolean transformed);

    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    LivingEntity getOriginalMob();
    void setOriginalMob(LivingEntity mob);

    MimicEntity.MimicAnimationState getMimicState();
    void setMimicState(MimicEntity.MimicAnimationState state);

    boolean isBiting();
    void setBiting(boolean biting);

    void saveNBTData(CompoundTag nbt);
    void loadNBTData(CompoundTag nbt);

    // サーバーからクライアントへ状態を同期するためのヘルパーメソッド
    void syncToClient(ServerPlayer player);
}