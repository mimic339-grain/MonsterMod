package net.mimic.monstermod.common.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

public interface IMorphCapability extends INBTSerializable<CompoundTag> {
    @Nullable String getMorphEntityTypeId();
    void setMorphEntityTypeId(@Nullable String entityTypeId);

    @Nullable LivingEntity getMorphEntity(Level level);
    void setMorphEntity(@Nullable LivingEntity entity);

    void morphInto(@Nullable String entityTypeId, Player player);
    void unmorph(Player player);

    boolean isMimicking();
    void setMimicking(boolean mimicking);
}