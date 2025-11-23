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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * PlayerTransformation 完全版
 * - サーバ authoritative
 * - HP / 属性管理
 * - クライアントは同期パケットで表示
 * - PlayerRendererMixin は identity を描画する
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;

    // ===== Getters / Flags =====
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean t) { isTransformed = t; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation id) { transformedMobId = id; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public @Nullable BaseMonsterIdentity getIdentity() { return identity; }
    // ==============================
    // 変身開始
    // ==============================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return;
        Level level = player.level();
        double currentHP = player.getHealth();

        // HP 保存
        if (isTransformed && identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), Math.max(0.0, currentHP));
            MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
        } else {
            MonsterTransformUtil.setPlayerHP(player, currentHP);
            MonsterTransformUtil.savePlayerHPToNBT(player);
        }

        if (!level.isClientSide) {
            // dummyEntity 生成
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {transformedEntity = (BaseMonsterEntity) type.create(level);}

            // Identity 生成
            identity = ensureIdentity(level, transformedEntity, player);

            // DEV に属性を反映
            if (transformedEntity != null) {
                MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);
            }

            // Identity HP 取得・Player に適用
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
            // クライアント側
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }
    }

    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        // 1. HP 保存
        if (identity != null) {
            double currentHP = player.getHealth();
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), Math.max(0.0, currentHP));
            MonsterTransformUtil.saveIdentityHPToNBT(player, identity.getId());
        }

        // 2. 属性とHPを復元
        MonsterTransformUtil.resetAttributesToPlayer(player, player);
        double prevHP = MonsterTransformUtil.getPlayerHP(player);
        player.setHealth((float)Math.min(prevHP, player.getAttributeValue(Attributes.MAX_HEALTH)));

        // 3. Hitbox更新前に Capability を完全リセット
        isTransformed = false;
        transformedEntity = null;
        identity = null;
        transformedMobId = null;

        // 4. サーバー側もクライアント側も正しい Hitbox に更新
        player.refreshDimensions();

        // 5. NBT 保存・クライアント同期
        MonsterTransformUtil.saveAllToNBT(player);
        syncToAllClients(player);
    }



    // ==============================
    // Identity / Entity生成
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
    // サーバ → クライアント同期
    // ==============================
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

    // ==============================
    // serialize / deserialize
    // ==============================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag, Player player) {
        if (player == null || tag == null) return;

        boolean newState = tag.getBoolean("isTransformed");
        String idStr = tag.getString("mobId");
        Level level = player.level();

        if (!newState) {
            // 変身解除
            isTransformed = false;
            transformedMobId = null;
            transformedEntity = null;
            identity = null;

            // サーバー側のHitbox更新
            if (!level.isClientSide) {
                player.refreshDimensions();
            }
            return;
        }

        // 変身状態
        isTransformed = true;
        transformedMobId = idStr.isEmpty() ? null : new ResourceLocation(idStr);

        // dummyEntity / Identity を用意
        transformedEntity = ensureEntity(level);
        identity = ensureIdentity(level, transformedEntity, player);

        // サーバー側でもHitbox更新
        if (!level.isClientSide) {
            player.refreshDimensions();
        }
    }
}
