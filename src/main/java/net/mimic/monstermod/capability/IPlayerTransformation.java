package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * プレイヤーの変身状態を管理するためのインターフェース。
 * BaseState は廃止済み。
 */
public interface IPlayerTransformation {

    boolean hasSavedOriginalStats();
    void setOriginalHealth(double hp);
    void setOriginalMaxHealth(double maxHp);
    void setOriginalAttackDamage(double dmg);
    void setOriginalArmor(double armor);
    void setOriginalMoveSpeed(double speed);

    double getOriginalHealth();
    double getOriginalMaxHealth();
    double getOriginalAttackDamage();
    double getOriginalArmor();
    double getOriginalMoveSpeed();

    void clearOriginalStats();

    @Nullable
    Entity getTransformedEntity();
    void setTransformedEntity(@Nullable Entity entity);

    boolean isTransformed();
    void setTransformed(boolean transformed);

    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    PlayerTransformation.MonsterState getMonsterState(ResourceLocation mobId);
    void setMonsterState(ResourceLocation mobId, PlayerTransformation.MonsterState state);

    MimicEntity.MimicAnimationState getAnimationState(ResourceLocation transformedMobId);

    void syncToClient(Player player);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    @Nullable
    IPlayerIdentity getTransformedIdentity();

    boolean isNoKnockback();
    void setNoKnockback(boolean value);
}
