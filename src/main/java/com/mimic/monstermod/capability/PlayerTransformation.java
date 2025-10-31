package com.mimic.monstermod.capability;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * PlayerTransformation 完全版
 * - BaseMonsterIdentity + AnimationPlayerTemplate + BaseMonsterEntity に対応
 * - 入力・アニメーションはすべて BaseMonsterIdentity に委譲
 * - サーバー／クライアント同期を完全サポート
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
    public void setTransformed(boolean transformed) {
        this.isTransformed = transformed;
    }

    public void setTransformedMobId(@Nullable ResourceLocation mobId) {
        this.transformedMobId = mobId;
    }
    // -----------------------------
// Tick統合（サーバ／クライアント両方）
// -----------------------------
    public void tick(Player player, float deltaSeconds) {
        if (!isTransformed || identity == null) return;
        identity.tick(player, deltaSeconds);
    }

    // -----------------------------
    // Transformation
    // -----------------------------
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;

        this.isTransformed = true;
        this.transformedMobId = mobId;
        this.identity = new BaseMonsterIdentity(mobId, abilitySlotCount);

        // 描画用Entityはまだ作らない
        this.entity = null;
        identity.setEntity(null);

        // 初期アニメーション
        identity.playAnimation("idle", true, 0f, 0f);

        syncToClient(player);
    }

    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        this.isTransformed = false;
        this.transformedMobId = null;
        if (identity != null) identity.setEntity(null);
        this.identity = null;
        this.entity = null;

        syncToClient(player);
    }

    // -----------------------------
    // Entity連携
    // -----------------------------
    public void attachEntity(BaseMonsterEntity entity) {
        this.entity = entity;
        if (identity != null) identity.setEntity(entity);
    }

    // -----------------------------
    // Identity 入力委譲
    // -----------------------------
    public void handleSkillInput(int skillIndex) {
        if (identity != null) identity.handleClientInput(null, skillIndex);
    }

    public void setPendingAttack(boolean attack) {
        if (identity != null) identity.setPendingAttack(attack);
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
            identity.setEntity(null);
            identity.playAnimation("idle", true, 0f, 0f);
        } else {
            this.identity = null;
            this.entity = null;
        }
    }

    // -----------------------------
    // Client Sync（サーバー → クライアント）
    // -----------------------------
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        ModMessages.sendToPlayer(new S2CTransformSyncPacket(player.getUUID(), serializeNBT()), sp);
    }
}
