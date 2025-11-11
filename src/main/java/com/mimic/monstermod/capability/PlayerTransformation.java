package com.mimic.monstermod.capability;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * PlayerTransformation — 完全版 YSMMOD対応
 *
 * - Identity / Entity / BoneMap の初期化を完全保証
 * - アニメーション呼び出しも安全に行う
 * - NBT 保存/復元・クライアント同期対応
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterIdentity identity = null;
    @Nullable private BaseMonsterEntity entity = null;
    private int abilitySlotCount = 3;

    // -----------------------------
    // Getter
    // -----------------------------
    public boolean isTransformed() { return isTransformed; }
    @Nullable public ResourceLocation getMobId() { return transformedMobId; }
    @Nullable public BaseMonsterIdentity getIdentity() { return identity; }
    @Nullable public BaseMonsterEntity getEntity() { return entity; }
    public void setAbilitySlotCount(int count) { this.abilitySlotCount = count; }
    public void setTransformed(boolean transformed) { this.isTransformed = transformed; }
    public void setTransformedMobId(@Nullable ResourceLocation mobId) { this.transformedMobId = mobId; }

    // -----------------------------
    // Tick
    // -----------------------------
    public void tick(Player player, float deltaSeconds) {
        if (!isTransformed || identity == null) return;
        identity.tick(player, deltaSeconds);
    }

    // -----------------------------
    // 変身開始
    // -----------------------------
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;

        MonsterMod.LOGGER.debug("[PlayerTransformation] startTransformation called for {}", player.getName().getString());

        this.isTransformed = true;
        this.transformedMobId = mobId;
        this.identity = new BaseMonsterIdentity(mobId, abilitySlotCount);

        // アニメーション初期化
        identity.playAnimation("idle", true, 0f, 0f);

        Level level = player.level();

        if (!level.isClientSide) {
            MimicEntity newEntity = new MimicEntity(ModEntitieType.MIMIC.get(), level);
            newEntity.setPos(player.getX(), player.getY(), player.getZ());
            attachEntity(newEntity);
            level.addFreshEntity(newEntity);
            MonsterMod.LOGGER.info("[PlayerTransformation] Server entity spawned & Identity attached: {}", newEntity.getUUID());
        } else {
            ensureClientEntity(player);
        }

        syncToClient(player);
    }

    // -----------------------------
    // 変身解除
    // -----------------------------
    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        MonsterMod.LOGGER.debug("[PlayerTransformation] stopTransformation called for {}", player.getName().getString());

        this.isTransformed = false;
        this.transformedMobId = null;

        if (identity != null) identity.setEntity(null);
        this.identity = null;

        if (entity != null) {
            entity.remove(BaseMonsterEntity.RemovalReason.DISCARDED);
            entity = null;
        }

        syncToClient(player);
    }

    // -----------------------------
    // Entity attach / Identity attach
    // -----------------------------
    public void attachEntity(BaseMonsterEntity entity) {
        MonsterMod.LOGGER.debug("[attachEntity] Attaching entity: {}", entity);
        this.entity = entity;

        if (identity != null) {
            entity.ensureModelInitialized();
            identity.setEntity(entity);
            if (identity.boneMap == null || identity.boneMap.isEmpty()) identity.autoInitBoneMap(entity);
            entity.setIdentity(identity);
            MonsterMod.LOGGER.info("[attachEntity] Identity attached to entity: {}", entity.getType().toShortString());
        }
    }

    // -----------------------------
    // クライアント用 Entity 生成
    // -----------------------------
    private void ensureClientEntity(Player player) {
        if (entity != null || identity == null) return;

        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        MimicEntity clientEntity = new MimicEntity(ModEntitieType.MIMIC.get(), clientLevel);
        clientEntity.setPos(player.getX(), player.getY(), player.getZ());

        attachEntity(clientEntity);
        clientLevel.addFreshEntity(clientEntity);

        if (identity.getAnimationTime() <= 0f) {
            identity.playAnimation("idle", true, 0f, 0f);
        }
    }

    // -----------------------------
    // 入力委譲
    // -----------------------------
    public void handleSkillInput(int skillIndex) {
        if (identity != null) identity.handleClientInput(null, skillIndex);
    }

    public void setPendingDodge(boolean dodge) {
        if (identity != null) identity.setPendingDodge(dodge);
    }

    public void handleMenuInput(Player player) {
        if (identity != null) identity.handleMenuInput(player);
    }

    // -----------------------------
    // NBT保存 / 復元
    // -----------------------------
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        tag.putInt("abilitySlotCount", abilitySlotCount);
        if (identity != null) tag.put("identity", identity.serializeNBT());
        return tag;
    }

    public void deserializeNBT(Player player, CompoundTag tag) {
        this.isTransformed = tag.getBoolean("isTransformed");
        this.transformedMobId = tag.contains("mobId") ? new ResourceLocation(tag.getString("mobId")) : null;
        this.abilitySlotCount = tag.getInt("abilitySlotCount");

        if (isTransformed && transformedMobId != null) {
            this.identity = new BaseMonsterIdentity(transformedMobId, abilitySlotCount);
            if (tag.contains("identity")) identity.deserializeNBT(tag.getCompound("identity"));
            if (identity.getAnimationTime() <= 0f) identity.playAnimation("idle", true, 0f, 0f);
            ensureClientEntity(player);
        } else {
            this.identity = null;
            this.entity = null;
        }
    }

    // -----------------------------
    // クライアント同期
    // -----------------------------
    public void syncToClient(Player player) {
        if (player instanceof ServerPlayer sp) {
            ModMessages.sendToPlayer(new S2CTransformSyncPacket(player.getUUID(), serializeNBT()), sp);
        }
    }
}
