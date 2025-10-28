package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;
    // ===== Setter / Getter =====
    public boolean isTransformed() { return isTransformed; }
    @Nullable public BaseMonsterEntity getEntity() { return transformedEntity; }
    @Nullable public BaseMonsterIdentity getIdentity() { return identity; }

    public void setTransformed(boolean transformed) {
        this.isTransformed = transformed;
    }

    public void setTransformedMobId(@Nullable ResourceLocation mobId) {
        this.transformedMobId = mobId;
    }

    @Nullable
    public ResourceLocation getTransformedMobId() {
        return this.transformedMobId;
    }

    public void tick(Player player) {
        if (!isTransformed) return;
        Level level = player.level();
        if (level == null) return;

        BaseMonsterEntity entity = ensureEntity(level);
        if (entity == null) return;

        BaseMonsterIdentity id = ensureIdentity(level, entity, player);

        if (!level.isClientSide) {
            id.tickServer();
            entity.getMonsterData().tick();
        } else {
            id.copyFromPlayerClient(player);
        }
    }

    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;
        isTransformed = true;
        transformedMobId = mobId;

        Level level = player.level();
        if (level != null) {
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {
                transformedEntity = (BaseMonsterEntity) type.create(level);
                if (transformedEntity != null) {
                    transformedEntity.moveTo(player.getX(), player.getY(), player.getZ(),
                            player.getYRot(), player.getXRot());
                    if (!level.isClientSide) level.addFreshEntity(transformedEntity);
                }
            }
        }

        ensureIdentity(level, transformedEntity, player);
        if (!level.isClientSide) syncToAllClients(player);
    }

    public void stopTransformation(Player player) {
        if (!isTransformed) return;
        isTransformed = false;

        if (transformedEntity != null) {
            transformedEntity.discard();
            transformedEntity = null;
        }
        identity = null;
        transformedMobId = null;

        if (!player.level().isClientSide) syncToAllClients(player);
    }

    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    public void syncToAllClients(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    @Nullable
    private BaseMonsterEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;
        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    private BaseMonsterIdentity ensureIdentity(Level level, BaseMonsterEntity entity, @Nullable Player player) {
        if (identity == null && entity != null) {
            identity = new BaseMonsterIdentity(entity, 3);
        }
        return identity;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        if (identity != null) tag.put("identity", identity.serializeNBT());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag, Level level) {
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("mobId") ? new ResourceLocation(tag.getString("mobId")) : null;

        if (isTransformed && transformedMobId != null && level != null) {
            BaseMonsterEntity entity = ensureEntity(level);
            identity = ensureIdentity(level, entity, null);
            if (tag.contains("identity") && identity != null) {
                identity.deserializeNBT(tag.getCompound("identity"));
            }
        }
    }
}
