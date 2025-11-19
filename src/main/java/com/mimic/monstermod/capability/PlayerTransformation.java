package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import com.mimic.monstermod.util.MonsterTransformUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * PlayerTransformation（完全版・MonsterTransformUtil準拠）
 * - startTransformation / stopTransformation で HP/Attribute を Map と NBT に保存
 * - DEV（実際の Player エンティティ）には Identity の属性を反映
 * - サーバ authoritative、クライアントは同期パケットで表示を更新
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;
    private boolean needsDimensionRefresh = false;

    // ===== Getters / Flags =====
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean t) { isTransformed = t; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation id) { transformedMobId = id; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public @Nullable BaseMonsterIdentity getIdentity() { return identity; }
    public void markDimensionDirty() { needsDimensionRefresh = true; }
    public boolean consumeDimensionRefresh() { boolean b = needsDimensionRefresh; needsDimensionRefresh = false; return b; }

    // ==============================
    // startTransformation
    // ==============================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return;
        Level level = player.level();
        double currentHP = player.getHealth();

        // HP 保存処理
        if (isTransformed) {
            // 変身中の場合 → Identity → Identity の変身
            if (identity != null) {
                MonsterTransformUtil.setIdentityHP(player, identity.getId(), Math.max(0.0, currentHP));
                MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
            }
        } else {
            // 変身していない場合 → Player → Identity の変身
            MonsterTransformUtil.setPlayerHP(player, currentHP);
            MonsterTransformUtil.savePlayerHPToNBT(player);
        }

        // サーバ側でEntity生成
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

            identity = ensureIdentity(level, transformedEntity, player);

            if (transformedEntity != null) {
                MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);
            }

            if (identity != null) {
                double identityHP = MonsterTransformUtil.getIdentityHP(player, identity.getId());
                MonsterTransformUtil.setIdentityHP(player, identity.getId(), identityHP);
                player.setHealth((float)Math.min(identityHP, player.getMaxHealth()));
            }

            isTransformed = true;
            transformedMobId = mobId;

            MonsterTransformUtil.saveAllToNBT(player);

            // サーバー → 全クライアントに同期
            CompoundTag nbt = S2CTransformSyncPacket.createNBT(player);
            ModMessages.sendToAllClients(new S2CTransformSyncPacket(player.getUUID(), nbt));
        } else {
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }

        markDimensionDirty();
    }


    // ==============================
    // stopTransformation
    // ==============================
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        // IdentityHPをMapに保存
        if (identity != null) {
            double currentDevHP = player.getHealth();
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), Math.max(0.0, currentDevHP));
            // NBTにも保存
            MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
        }

        // Player属性を元に戻す
        MonsterTransformUtil.resetAttributesToPlayer(player, player);

        // PlayerHPをMapから復元
        double prevPlayerHP = MonsterTransformUtil.getPlayerHP(player);
        double clampedPrev = Math.min(prevPlayerHP, player.getAttributeValue(Attributes.MAX_HEALTH));
        player.setHealth((float)Math.max(0.0, clampedPrev));

        // NBTにHP/Attributeを保存
        MonsterTransformUtil.saveAllToNBT(player);

        // Entity破棄（サーバ側）
        if (transformedEntity != null && !player.level().isClientSide) {
            transformedEntity.discard();
        }
        transformedEntity = null;
        identity = null;
        transformedMobId = null;
        isTransformed = false;

        // サーバー → 全クライアントに同期
        if (!player.level().isClientSide) {
            CompoundTag nbt = S2CTransformSyncPacket.createNBT(player);
            ModMessages.sendToAllClients(new S2CTransformSyncPacket(player.getUUID(), nbt));
        }

        markDimensionDirty();
    }

    // ==============================
    // ensureIdentity / ensureEntity
    // ==============================
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

    // ==============================
    // syncToClient
    // ==============================
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());

        // Mapに保存されたHPを送信
        double playerHP = MonsterTransformUtil.getPlayerHP(player);
        double identityHP = identity != null ? MonsterTransformUtil.getIdentityHP(player, identity.getId()) : player.getHealth();
        nbt.putDouble("playerHP", playerHP);
        nbt.putDouble("identityHP", identityHP);

        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    // ==============================
    // serialize/deserialize capability NBT
    // ==============================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("mobId") && !tag.getString("mobId").isEmpty() ? new ResourceLocation(tag.getString("mobId")) : null;
    }
}
