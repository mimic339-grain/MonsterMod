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
 * PlayerTransformation 完全版
 * - サーバー authoritative
 * - HP/属性管理
 * - クライアントはNBT反映と寸法表示のみ
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;
    private boolean needsDimensionRefresh = false;

    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean t) { isTransformed = t; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation id) { transformedMobId = id; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public @Nullable BaseMonsterIdentity getIdentity() { return identity; }
    public void markDimensionDirty() { needsDimensionRefresh = true; }
    public boolean consumeDimensionRefresh() { boolean b = needsDimensionRefresh; needsDimensionRefresh = false; return b; }
    // ================================
// 変身開始（サーバー側）
// ================================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed || player == null || mobId == null) return;

        Level level = player.level();
        UUID uuid = player.getUUID();
        float currentPlayerHP = player.getHealth();

        // 変身前HP保存
        if (isTransformed && identity != null) {
            MonsterTransformUtil.setIdentityHP(uuid, identity, currentPlayerHP);
        } else if (!isTransformed) {
            MonsterTransformUtil.setPlayerPrevHP(uuid, currentPlayerHP);
        }

        // サーバー側のみ Entity 生成
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

            // Identity 生成
            identity = ensureIdentity(level, transformedEntity, player);

            // IdentityHP 取得 & Player 属性・HP に強制上書き
            if (identity != null) {
                float idHP = MonsterTransformUtil.getIdentityHP(uuid, identity);
                MonsterTransformUtil.copyAttributesToPlayer(player, identity, true);
                player.setHealth(idHP); // IdentityHPで上書き
            }

            isTransformed = true;
            transformedMobId = mobId;

            // サーバ→クライアント同期
            syncToClient(player);

        } else {
            // クライアント側
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }

        markDimensionDirty();
    }
    // ================================
// 変身解除（サーバー側）
// ================================
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        Level level = player.level();
        UUID uuid = player.getUUID();

        // IdentityHP 保存（現状の IdentityHP を強制保持）
        if (identity != null) {
            float currentHP = player.getHealth();
            MonsterTransformUtil.setIdentityHP(uuid, identity, currentHP);
        }

        // Player属性・HP復元
        MonsterTransformUtil.resetPlayerAttributes(player, true); // 属性リセット
        float prevHP = MonsterTransformUtil.getPlayerPrevHP(uuid); // 元のHPに復元
        player.setHealth(prevHP);

        // Entity破棄
        if (transformedEntity != null) transformedEntity.discard();
        transformedEntity = null;
        identity = null;
        transformedMobId = null;
        isTransformed = false;

        // サーバ→クライアント同期
        syncToClient(player);
        markDimensionDirty();
    }

    // ================================
    // Entity / Identity生成
    // ================================
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

    public void copyFrom(PlayerTransformation old, UUID uuid) {
        this.isTransformed = old.isTransformed;
        this.transformedMobId = old.transformedMobId;
        this.transformedEntity = null;
        this.identity = old.identity;
        this.needsDimensionRefresh = old.needsDimensionRefresh;
    }
    // ================================
    // サーバ→クライアント同期
    // ================================
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

    // ================================
    // NBT保存 / 復元（クライアント側）
    // ================================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        if (identity != null && identity.hasCurrentHP()) tag.putFloat("identityHP", identity.getCurrentHP());
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
            // 変身解除（クライアント側表示）
            isTransformed = false;
            transformedMobId = null;
            transformedEntity = null;
            identity = null;

            // HPを復元
            if (tag.contains("playerHealth")) {
                float prevHP = tag.getFloat("playerHealth");
                p.setHealth(prevHP);
                // MAX_HEALTH を元に戻す
                if (p.getAttribute(Attributes.MAX_HEALTH) != null) {
                    p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
                }
            }

            // 寸法/eyeHeight 更新フラグ
            markDimensionDirty();
            return;
        }

        // 変身中（クライアント側表示）
        isTransformed = true;
        transformedMobId = idStr.isEmpty() ? null : new ResourceLocation(idStr);
        transformedEntity = ensureEntity(level);
        identity = ensureIdentity(level, transformedEntity, p);

        float maxHP = transformedEntity != null ? (float) transformedEntity.getAttributeValue(Attributes.MAX_HEALTH) : 20f;
        float hpToApply = storedHP > 0 ? storedHP : maxHP;

        // 表示HPだけ反映
        if (p.getAttribute(Attributes.MAX_HEALTH) != null) {
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);
            p.setHealth(Math.min(hpToApply, maxHP));
        }

        // 寸法/eyeHeight 更新フラグ
        markDimensionDirty();
    }
}
