package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.identity.impl.MimicIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * プレイヤー変身管理
 *
 * - IdentityRegistry を利用して Entity に応じた Identity を自動生成
 * - 変身中はプレイヤー状態を Entity に完全同期
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;

    // ===== Getter / Setter =====
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean transformed) { this.isTransformed = transformed; }

    @Nullable public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation mobId) { this.transformedMobId = mobId; }

    @Nullable public BaseMonsterEntity getEntity() { return transformedEntity; }
    public void setTransformedEntity(@Nullable BaseMonsterEntity entity) { this.transformedEntity = entity; }

    @Nullable public BaseMonsterIdentity getIdentity() { return identity; }
    // ===== Tick =====
    public void tick(Player player) {
        if (!isTransformed) return;

        Level level = player.level();
        BaseMonsterEntity entity = ensureEntity(level);
        if (entity == null) return;

        BaseMonsterIdentity id = ensureIdentity(level, entity, player);

        if (!level.isClientSide) {
            // サーバ側: クールダウンや状態更新
            id.tickServer(player);
            if (entity.getMonsterData() != null) entity.getMonsterData().tick();
            id.copyFromPlayerServer(player);
        } else {
            // クライアント側: プレイヤー状態を反映
            id.copyFromPlayerClient(player);
        }
    }

    // ===== 変身開始 =====
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;
        isTransformed = true;
        transformedMobId = mobId;
        Level level = player.level();

        // サーバ側 Entity 生成
        if (!level.isClientSide) {
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {
                transformedEntity = (BaseMonsterEntity) type.create(level);
                if (transformedEntity != null) {
                    transformedEntity.moveTo(player.position());
                    transformedEntity.setYRot(player.getYRot());
                    transformedEntity.setXRot(player.getXRot());
                    transformedEntity.setYHeadRot(player.getYHeadRot());
                    level.addFreshEntity(transformedEntity);
                }
            }
        } else {
            // クライアント側: 仮想 Entity 生成
            transformedEntity = ensureEntity(level);
        }

        ensureIdentity(level, transformedEntity, player);
        syncToClient(player);
    }

    // ===== 変身解除 =====
    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        isTransformed = false;

        if (transformedEntity != null) {
            transformedEntity.discard();
            transformedEntity = null;
        }

        identity = null;
        transformedMobId = null;

        syncToClient(player);
    }

    // ===== クライアント同期 =====
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    // ===== Entity 確保 =====
    @Nullable
    private BaseMonsterEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;
        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    // ===== Identity 確保 =====
    private BaseMonsterIdentity ensureIdentity(Level level, BaseMonsterEntity entity, Player player) {
        if (identity != null) return identity;

        identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, entity);
        if (identity == null) {
            identity = (entity instanceof MimicEntity) ? new MimicIdentity(entity) : new BaseMonsterIdentity(entity, 3);
        }

        if (!level.isClientSide && player != null) {
            identity.copyFromPlayerServer(player);
        }

        return identity;
    }

    // ===== NBT 保存 =====
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        return tag;
    }

    // ===== NBT 復元 =====
    public void deserializeNBT(CompoundTag tag) {
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("mobId") ? new ResourceLocation(tag.getString("mobId")) : null;

        if (isTransformed && transformedMobId != null && identity == null) {
            Level level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.isClientSide) {
                BaseMonsterEntity entity = ensureEntity(level);
                if (entity != null) {
                    identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, entity);
                    if (identity == null) identity = new BaseMonsterIdentity(entity, 3);
                }
            }
        }
    }

    // ===== Identity 更新（外部用） =====
    public void setIdentity(@Nullable BaseMonsterIdentity identity) {
        this.identity = identity;
    }
}
