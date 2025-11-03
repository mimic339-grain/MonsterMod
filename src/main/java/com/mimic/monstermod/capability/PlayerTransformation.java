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
 * PlayerTransformation 完全版（YSMMOD方式・Identity同期改良）
 *
 * - Entity生成前にIdentityをattachし、描画タイミングのズレを完全解消
 * - サーバー・クライアント双方で確実にIdentityが結びついた状態を維持
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterIdentity identity = null;
    private int abilitySlotCount = 3;
    @Nullable private BaseMonsterEntity entity = null;

    // -----------------------------
    // Getter / Setter
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

        this.isTransformed = true;
        this.transformedMobId = mobId;

        // Identity生成
        this.identity = new BaseMonsterIdentity(mobId, abilitySlotCount);
        identity.playAnimation("idle", true, 0f, 0f);

        Level level = player.level();

        if (!level.isClientSide) {
            // -----------------------------
            // サーバー側
            // -----------------------------
            MimicEntity newEntity = new MimicEntity(ModEntitieType.MIMIC.get(), level);

            // ✅ 先にIdentityをattach（spawn前）
            attachEntity(newEntity);

            // 座標セット後にspawn
            newEntity.setPos(player.getX(), player.getY(), player.getZ());
            level.addFreshEntity(newEntity);

            MonsterMod.LOGGER.info("[PlayerTransformation] Spawned and attached mimic on server side: {}", newEntity.getUUID());
        } else {
            // -----------------------------
            // クライアント側
            // -----------------------------
            ensureClientEntity();
        }

        syncToClient(player);
    }

    // -----------------------------
    // 変身解除
    // -----------------------------
    public void stopTransformation(Player player) {
        if (!isTransformed) return;
        this.isTransformed = false;
        this.transformedMobId = null;

        if (identity != null) identity.setEntity(null);
        this.identity = null;

        if (entity != null) {
            entity.remove(BaseMonsterEntity.RemovalReason.DISCARDED);
            MonsterMod.LOGGER.info("[PlayerTransformation] Entity removed: {}", entity);
        }
        this.entity = null;

        syncToClient(player);
    }

    // -----------------------------
    // Entity attach
    // -----------------------------
    public void attachEntity(BaseMonsterEntity entity) {
        this.entity = entity;
        if (identity != null) {
            entity.ensureModelInitialized();     // モデル初期化
            identity.setEntity(entity);
            identity.autoInitBoneMap(entity);    // BoneMap 初期化
            entity.setIdentity(identity);        // Renderer参照用
            MonsterMod.LOGGER.info("[attachEntity] Identity & model attached to entity: {}", entity.getType().toShortString());
        } else {
            MonsterMod.LOGGER.warn("[attachEntity] Tried to attach entity but identity is null");
        }
    }

    // -----------------------------
    // クライアント用Entity生成
    // -----------------------------
    private void ensureClientEntity() {
        if (entity == null && identity != null) {
            Level clientLevel = Minecraft.getInstance().level;
            if (clientLevel == null) return;

            MimicEntity clientEntity = new MimicEntity(ModEntitieType.MIMIC.get(), clientLevel);
            clientEntity.setPos(
                    Minecraft.getInstance().player.getX(),
                    Minecraft.getInstance().player.getY(),
                    Minecraft.getInstance().player.getZ()
            );

            // ✅ Identityを先にattach
            attachEntity(clientEntity);

            // ✅ その後すぐにワールドに追加（RendererがIdentityを即参照可能）
            clientLevel.addFreshEntity(clientEntity);

            // ✅ アニメーション再生はspawn後でもOK
            identity.playAnimation("idle", true, 0f, 0f);

            MonsterMod.LOGGER.info("[ensureClientEntity] Client mimic entity spawned & identity linked: {}", clientEntity.getUUID());
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
    // NBT保存/復元
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
            identity.playAnimation("idle", true, 0f, 0f);
            ensureClientEntity();
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
