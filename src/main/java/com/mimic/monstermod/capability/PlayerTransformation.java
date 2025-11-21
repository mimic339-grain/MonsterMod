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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * PlayerTransformation 完全版
 * - HP 個別管理 (playerHP / identityHP)
 * - 変身中の属性は DEV(BaseMonsterEntity) に完全依存
 * - サーバ authoritative
 * - 変身/解除後はパケット同期
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;
    private boolean needsDimensionRefresh = false;

    // ========= getter/setter =========
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean t) { isTransformed = t; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation id) { transformedMobId = id; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public @Nullable BaseMonsterIdentity getIdentity() { return identity; }
    public void markDimensionDirty() { needsDimensionRefresh = true; }
    public boolean consumeDimensionRefresh() { boolean b = needsDimensionRefresh; needsDimensionRefresh = false; return b; }

    // ================================//
    // 変身開始
    // ================================//
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return;

        Level level = player.level();
        double currentHP = player.getHealth();

        // HP 保存
        if (isTransformed && identity != null) {
            MonsterTransformUtil.logAlways("[startTransformation] saving old IdentityHP for id=" + identity.getId() + " hp=" + currentHP);
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), currentHP);
            MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
        } else {
            MonsterTransformUtil.logAlways("[startTransformation] saving PlayerHP before transform: hp=" + currentHP);
            MonsterTransformUtil.setPlayerHP(player, currentHP);
            MonsterTransformUtil.savePlayerHPToNBT(player);
        }

        if (!level.isClientSide) {
            // DEV(Entity) 生成（サーバー）
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) transformedEntity = (BaseMonsterEntity) type.create(level);

            // Identity 生成
            identity = ensureIdentity(level, transformedEntity, player);

            // Player に属性コピー
            if (transformedEntity != null) {
                MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);
            }

            // Identity HP 適用
            if (identity != null) {
                double hp = MonsterTransformUtil.getIdentityHP(player, identity.getId());
                player.setHealth((float) Math.min(hp, player.getMaxHealth()));
            }

            isTransformed = true;
            transformedMobId = mobId;

            MonsterTransformUtil.saveHPToNBT(player, new CompoundTag());
            syncToAllClients(player);
        } else {
            // クライアント側：描画用 DEV と Identity
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }

        // クライアント側もサーバ側も必ず目線・寸法更新
        MonsterTransformUtil.updateViewAndHitbox(player, true);
        markDimensionDirty();
    }

    // ================================//
    // 変身解除
    // ================================//
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        // Identity HP 保存
        if (identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), player.getHealth());
        }

        // Player 属性リセット
        MonsterTransformUtil.resetAttributesToPlayer(player);

        // Player HP 戻す
        double prevHP = MonsterTransformUtil.getPlayerHP(player);
        double maxHP = player.getAttributeValue(Attributes.MAX_HEALTH);
        player.setHealth((float) Math.min(prevHP, maxHP));

        // DEV削除（サーバ側のみ）
        if (transformedEntity != null && !player.level().isClientSide) {
            transformedEntity.discard();
        }
        transformedEntity = null;
        identity = null;
        transformedMobId = null;
        isTransformed = false;

        // NBT 保存
        MonsterTransformUtil.saveHPToNBT(player, new CompoundTag());

        // クライアント・サーバ共に目線・寸法更新
        MonsterTransformUtil.updateViewAndHitbox(player, false);
        markDimensionDirty();

        // 全クライアント同期
        syncToAllClients(player);
    }

    // ================================//
    // Identity / Entity 生成保証
    // ================================//
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
        if (type != null) {
            transformedEntity = (BaseMonsterEntity) type.create(level);
            // クライアント側なら位置復元など必要ならここで初期化
        }
        return transformedEntity;
    }

    // ================================//
    // サーバ → 全クライアント同期
    // ================================//
    public void syncToAllClients(Player player) {
        if (player.level().isClientSide) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));
        nbt.putDouble("identityHP",
                identity != null ? MonsterTransformUtil.getIdentityHP(player, identity.getId()) : player.getHealth());

        ModMessages.sendToAllClients(new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    public void syncToClient(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));
        nbt.putDouble("identityHP",
                identity != null ? MonsterTransformUtil.getIdentityHP(player, identity.getId()) : player.getHealth());

        ModMessages.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt)
        );
    }

    // ================================//
    // Capability serialize / deserialize
    // ================================//
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        boolean shouldTransform = tag.getBoolean("isTransformed");
        String idStr = tag.getString("mobId");
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (player == null || level == null) return;

        if (!shouldTransform) {
            isTransformed = false;
            transformedMobId = null;
            transformedEntity = null;
            identity = null;
            markDimensionDirty();
            MonsterTransformUtil.updateViewAndHitbox(player, false);
            return;
        }

        isTransformed = true;
        transformedMobId = idStr.isEmpty() ? null : new ResourceLocation(idStr);
        transformedEntity = ensureEntity(level);
        identity = ensureIdentity(level, transformedEntity, player);

        float maxHP = transformedEntity != null
                ? (float) transformedEntity.getAttributeValue(Attributes.MAX_HEALTH)
                : (float) player.getAttributeValue(Attributes.MAX_HEALTH);

        float applyHP = tag.contains("identityHP") ? tag.getFloat("identityHP") : maxHP;
        if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
            Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(maxHP);
            player.setHealth(Math.min(applyHP, maxHP));
        }

        markDimensionDirty();
        MonsterTransformUtil.updateViewAndHitbox(player, isTransformed);
    }
}
