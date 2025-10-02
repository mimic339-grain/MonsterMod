package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.capability.PlayerTransformation.MonsterState;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

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

    MonsterState getMonsterState(ResourceLocation mobId);
    void setMonsterState(ResourceLocation mobId, MonsterState state);

    MimicEntity.MimicAnimationState getAnimationState(ResourceLocation transformedMobId);

    int getAnimationTick(ResourceLocation mobId);
    void setAnimationTick(ResourceLocation mobId, int tick);

    void syncToClient(Player player);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    void setAnimationStateAndSync(ResourceLocation mobId, MimicEntity.MimicAnimationState state, ServerPlayer player);

    @Nullable
    IPlayerIdentity getTransformedIdentity();

    boolean isNoKnockback();
    void setNoKnockback(boolean value);

    void markSynced(ResourceLocation identityId, boolean transform, MimicEntity.MimicAnimationState animationState);

    boolean shouldSync(ResourceLocation identityId, boolean transform, MimicEntity.MimicAnimationState animationState, int animationTick);
}
