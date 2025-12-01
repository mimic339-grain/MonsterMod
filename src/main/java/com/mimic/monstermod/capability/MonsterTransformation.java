package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.mixin.accessor.EntityAccessor;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import com.mimic.monstermod.util.MonsterTransformUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MonsterTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;

    public boolean isTransformed() { return isTransformed; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public @Nullable BaseMonsterIdentity getIdentity() { return identity; }

    // =============================================================================
    // startTransformation
    // =============================================================================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return;
        Level level = player.level();

        double currentHP = player.getHealth();

        if (isTransformed && identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), Math.max(0.0, currentHP));
            MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
        } else {
            MonsterTransformUtil.setPlayerHP(player, currentHP);
            MonsterTransformUtil.savePlayerHPToNBT(player);
        }

        if (!level.isClientSide) {
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) transformedEntity = (BaseMonsterEntity) type.create(level);

            identity = ensureIdentity(level, transformedEntity, player);

            if (transformedEntity != null) {
                MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);
                //自動段差上昇 変更
                ((EntityAccessor) player).setMaxUpStep(transformedEntity.getStepHeightValue());
            }

            if (identity != null) {
                double identityHP = MonsterTransformUtil.getIdentityHP(player, identity.getId());
                MonsterTransformUtil.setIdentityHP(player, identity.getId(), identityHP);
                player.setHealth((float) Math.min(identityHP, player.getMaxHealth()));
            }

            isTransformed = true;
            transformedMobId = mobId;
            player.refreshDimensions();
            MonsterTransformUtil.saveAllToNBT(player);
            syncToAllClients(player);
        } else {
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }
    }

    // =============================================================================
    // stopTransformation（修正版）
    // =============================================================================
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        if (identity != null) {
            double currentHP = player.getHealth();
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), Math.max(0.0, currentHP));
            MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
        }

        MonsterTransformUtil.resetAttributesToPlayer(player, player);
        double prevHP = MonsterTransformUtil.getPlayerHP(player);
        player.setHealth((float) Math.min(prevHP, player.getAttributeValue(Attributes.MAX_HEALTH)));
        //自動段差上昇　デフォルト
        ((EntityAccessor) player).setMaxUpStep(0.6f);

        isTransformed = false;
        transformedEntity = null;
        identity = null;
        transformedMobId = null;

        player.refreshDimensions();

        MonsterTransformUtil.saveAllToNBT(player);
        syncToAllClients(player);
    }

    // =============================================================================
    // Identity / Entity
    // =============================================================================
    private BaseMonsterIdentity ensureIdentity(Level level, BaseMonsterEntity ent, Player player) {
        if (identity != null) return identity;
        identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, ent);
        if (identity == null && ent != null) identity = new BaseMonsterIdentity(ent, 3);

        if (!level.isClientSide && player != null && identity != null) identity.copyFromPlayerServer(player);
        return identity;
    }

    private BaseMonsterEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;

        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;

        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    // =============================================================================
    // Sync
    // =============================================================================
    public void syncToAllClients(Player player) {
        if (player.level().isClientSide) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));
        nbt.putDouble("identityHP", identity != null ? MonsterTransformUtil.getIdentityHP(player, identity.getId()) : player.getHealth());

        ModMessages.sendToAllClients(new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    public void syncToClient(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));
        nbt.putDouble("identityHP", identity != null ? MonsterTransformUtil.getIdentityHP(player, identity.getId()) : player.getHealth());

        ModMessages.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt)
        );
    }

    // =============================================================================
    // NBT（★deserializeNBT 修正版★）
    // =============================================================================
    public void onLoad(Player player) {
        if (player == null) return;

        if (!isTransformed || transformedMobId == null) {
            transformedEntity = null;
            identity = null;

            if (!player.level().isClientSide) player.refreshDimensions();
            return;
        }

        // Entity / Identity を生成
        transformedEntity = ensureEntity(player.level());
        identity = ensureIdentity(player.level(), transformedEntity, player);

        if (!player.level().isClientSide) player.refreshDimensions();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;

        isTransformed = tag.getBoolean("isTransformed");

        if (tag.contains("mobId")) {
            transformedMobId = new ResourceLocation(tag.getString("mobId"));
        } else {
            transformedMobId = null;
        }
        // ★ここでは Player や Level に依存する処理は絶対に入れない
        transformedEntity = null;
        identity = null;
    }
}
