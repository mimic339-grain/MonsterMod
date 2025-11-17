package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import com.mimic.monstermod.util.MonsterTransformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PlayerTransformation 完全版（HP独立管理対応）
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;
    private boolean needsDimensionRefresh = false;

    // ------------------------
    // Getter / Setter
    // ------------------------
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean t) { isTransformed = t; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation id) { transformedMobId = id; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public @Nullable BaseMonsterIdentity getIdentity() { return identity; }
    public void markDimensionDirty() { needsDimensionRefresh = true; }
    public boolean consumeDimensionRefresh() { boolean b = needsDimensionRefresh; needsDimensionRefresh = false; return b; }

    // =====================================================
    // 変身開始
    // =====================================================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (mobId == null) return;
        Level level = player.level();

        // 同じ Identity に再変身ならスキップ
        if (isTransformed && transformedMobId != null && transformedMobId.equals(mobId) && identity != null) return;

        // -----------------------------
        // 変身前HP保存
        // -----------------------------
        if (isTransformed && identity != null) {
            // 変身中 → 現在HPを IdentityHP に保存
            MonsterTransformUtil.setIdentityHP(player.getUUID(), identity, player.getHealth());
        } else if (!isTransformed) {
            // 未変身 → Player素HPを保存
            MonsterTransformUtil.setPlayerPrevHP(player.getUUID(), player.getHealth());
        }

        // -----------------------------
        // サーバ側のみEntity生成
        // -----------------------------
        if (!level.isClientSide) {
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {
                transformedEntity = (BaseMonsterEntity) type.create(level);
                if (transformedEntity != null) {
                    transformedEntity.moveTo(player.position());
                    transformedEntity.setYRot(player.getYRot());
                    transformedEntity.setXRot(player.getXRot());
                    transformedEntity.setYHeadRot(player.getYHeadRot());
                }
            }

            // Identity生成・取得
            identity = ensureIdentity(level, transformedEntity, player);

            // -----------------------------
            // IdentityHP → PlayerHPコピー（上書き）
            // -----------------------------
            if (identity != null) {
                float idHP = MonsterTransformUtil.getIdentityHP(player.getUUID(), identity);
                player.setHealth(idHP);
                // 属性コピー（HPは上書きしない）
                MonsterTransformUtil.copyAttributesToPlayer(player, identity, false);
            }

            // -----------------------------
            // クライアント同期
            // -----------------------------
            isTransformed = true;
            transformedMobId = mobId;
            syncToClient(player);

        } else {
            // クライアント側
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }

        markDimensionDirty();
    }

    // =====================================================
    // 変身解除
    // =====================================================
    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        // -----------------------------
        // Player属性・HP復元
        // -----------------------------
        MonsterTransformUtil.resetPlayerAttributes(player, true);
        float prevHP = MonsterTransformUtil.getPlayerPrevHP(player.getUUID());
        player.setHealth(prevHP);

        // -----------------------------
        // Entity破棄
        // -----------------------------
        if (transformedEntity != null) transformedEntity.discard();
        transformedEntity = null;
        identity = null;
        transformedMobId = null;
        isTransformed = false;

        // -----------------------------
        // クライアント同期
        // -----------------------------
        syncToClient(player);
        markDimensionDirty();
    }

    // =====================================================
    // Entity / Identity生成
    // =====================================================
    private BaseMonsterEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;
        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    private BaseMonsterIdentity ensureIdentity(Level level, BaseMonsterEntity ent, Player player) {
        if (identity != null) return identity;
        identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, ent);
        if (identity == null && ent != null) identity = new BaseMonsterIdentity(ent, 3);
        if (!level.isClientSide && player != null && identity != null) identity.copyFromPlayerServer(player);
        return identity;
    }

    // =====================================================
    // Capabilityコピー
    // =====================================================
    public void copyFrom(PlayerTransformation old, UUID uuid) {
        this.isTransformed = old.isTransformed;
        this.transformedMobId = old.transformedMobId;
        this.transformedEntity = null;
        this.identity = old.identity;
        this.needsDimensionRefresh = old.needsDimensionRefresh;
    }

    // =====================================================
    // 同期（サーバ→クライアント）
    // =====================================================
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        nbt.putFloat("playerHealth", player.getHealth());

        if (identity != null) {
            float idHP = MonsterTransformUtil.getIdentityHP(player.getUUID(), identity);
            nbt.putFloat("identityHP", idHP);
        }

        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    // =====================================================
    // NBT保存・復元
    // =====================================================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        if (identity != null && identity.hasCurrentHP())
            tag.putFloat("identityHP", identity.getCurrentHP());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        boolean newState = tag.getBoolean("isTransformed");
        String idStr = tag.getString("mobId");
        float storedHP = tag.contains("identityHP") ? tag.getFloat("identityHP") : -1f;

        Player p = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (p == null || level == null) return;

        if (!newState) {
            isTransformed = false;
            transformedMobId = null;
            transformedEntity = null;
            identity = null;
            MonsterTransformUtil.resetPlayerAttributes(p, true);
            markDimensionDirty();
            return;
        }

        isTransformed = true;
        transformedMobId = idStr.isEmpty() ? null : new ResourceLocation(idStr);
        transformedEntity = ensureEntity(level);
        identity = ensureIdentity(level, transformedEntity, p);

        float maxHP = transformedEntity != null ? (float) transformedEntity.getAttributeValue(Attributes.MAX_HEALTH) : 20f;
        float hpToApply = storedHP > 0 ? storedHP : maxHP;

        if (identity != null) {
            MonsterTransformUtil.setIdentityHP(p.getUUID(), identity, hpToApply);
        }

        if (p.getAttribute(Attributes.MAX_HEALTH) != null) {
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);
            // クライアントでは PlayerHP は表示用に IdentityHP を反映
            p.setHealth(Math.min(hpToApply, maxHP));
        }

        if (transformedEntity != null && identity != null)
            MonsterTransformUtil.copyAttributesToPlayer(p, identity, false);
    }
}
