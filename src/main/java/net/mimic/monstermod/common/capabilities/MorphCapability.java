package net.mimic.monstermod.common.capabilities;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.network.PacketHandler;
import net.mimic.monstermod.network.morph.MorphSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class MorphCapability implements IMorphCapability {
    @Nullable
    private String morphEntityTypeId;
    @Nullable
    private transient LivingEntity cachedMorphEntity;

    private boolean isMimicking = false;

    @Override
    public @Nullable String getMorphEntityTypeId() {
        return morphEntityTypeId;
    }

    @Override
    public void setMorphEntityTypeId(@Nullable String entityTypeId) {
        this.morphEntityTypeId = entityTypeId;
        this.cachedMorphEntity = null;
    }

    @Override
    public @Nullable LivingEntity getMorphEntity(Level level) {
        if (morphEntityTypeId == null) {
            return null;
        }
        if (cachedMorphEntity == null) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(morphEntityTypeId));
            if (entityType != null) {
                cachedMorphEntity = (LivingEntity) entityType.create(level);
            }
        }
        return cachedMorphEntity;
    }

    @Override
    public void setMorphEntity(@Nullable LivingEntity entity) {
        this.cachedMorphEntity = entity;
        this.morphEntityTypeId = entity != null ? entity.getEncodeId() : null;
    }

    @Override
    public void morphInto(@Nullable String entityTypeId, Player player) {
        setMorphEntityTypeId(entityTypeId);
        if (player instanceof ServerPlayer playerSP) {
            PacketHandler.sendToPlayer(new MorphSyncPacket(player.getId(), entityTypeId), playerSP);
        }
    }

    @Override
    public void unmorph(Player player) {
        morphInto(null, player);
        this.isMimicking = false;
    }

    @Override
    public boolean isMimicking() {
        return isMimicking;
    }

    @Override
    public void setMimicking(boolean mimicking) {
        this.isMimicking = mimicking;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (morphEntityTypeId != null) {
            nbt.putString("MorphEntityTypeId", morphEntityTypeId);
        }
        nbt.putBoolean("IsMimicking", isMimicking);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("MorphEntityTypeId")) {
            this.morphEntityTypeId = nbt.getString("MorphEntityTypeId");
        } else {
            this.morphEntityTypeId = null;
        }
        this.isMimicking = nbt.getBoolean("IsMimicking");
    }
}