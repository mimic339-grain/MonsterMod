package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public interface IPlayerTransformation {

    boolean isTransformed();
    void setTransformed(boolean transformed);

    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    @Nullable
    BaseMonsterEntity<?> getTransformedEntity();
    void setTransformedEntity(@Nullable BaseMonsterEntity<?> entity);

    PlayerTransformation.MonsterState getMonsterState(ResourceLocation mobId);
    void setMonsterState(ResourceLocation mobId, PlayerTransformation.MonsterState state);

    boolean isNoKnockback();
    void setNoKnockback(boolean value);

    @Nullable
    BaseMonsterEntity<?> getClientTransformedEntity();
    void setClientTransformedEntity(@Nullable BaseMonsterEntity<?> entity);

    void syncToClient(Player player);
}
