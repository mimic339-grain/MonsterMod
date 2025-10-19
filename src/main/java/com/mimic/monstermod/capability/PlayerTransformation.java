package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * プレイヤー変身情報の管理クラス（IDENTITYMOD方式完全版）
 * - 状態管理のみ担当
 * - 描画・回転同期は Client Tick で毎フレームコピー
 * - サーバー側でクールタイム管理
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable
    private ResourceLocation transformedMobId = null;
    @Nullable
    private BaseMonsterEntity transformedEntity = null;
    @Nullable
    private BaseMonsterIdentity identity = null;

    // --- Getter / Setter ---
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean transformed) { this.isTransformed = transformed; }
    public void setTransformedMobId(@Nullable ResourceLocation mobId) { this.transformedMobId = mobId; }

    // --- サーバー→クライアント同期 ---
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    // --- Tick処理 ---
    public void tick(Player player) {
        if (!isTransformed) return;

        Level level = player.getCommandSenderWorld();

        // 1. Identity取得（生成済みでなければ生成）
        BaseMonsterIdentity identity = getIdentity(level, player);
        if (identity == null) return;

        if (!level.isClientSide) {
            // 2. サーバー側: クールタイム減算
            for (int i = 0; i < 3; i++) {
                int cd = identity.getCooldown(i);
                if (cd > 0) identity.setCooldown(i, cd - 1);
            }

            // 3. 元EntityのMonsterDataにも反映（必要であれば）
            BaseMonsterEntity entity = getTransformedEntity(level);
            if (entity != null) {
                IMonsterData data = entity.getMonsterData();
                if (data != null) {
                    data.setAbilityCooldown(Math.max(0, data.getAbilityCooldown() - 1));
                    data.setRemainingHostilityTime(Math.max(0, data.getRemainingHostilityTime() - 1));
                }
            }

            // サーバー用コピー（回転・装備・位置・速度）
            identity.copyFromPlayerServer(player);
        } else {
            // 4. クライアント側: 毎フレーム Player → Identity コピー
            identity.copyFromPlayerClient(player);
        }
    }

    // --- Identity取得 ---
    @Nullable
    public BaseMonsterIdentity getIdentity(Level level, Player player) {
        if (!isTransformed || transformedMobId == null) return null;

        if (identity == null) {
            BaseMonsterEntity entity = getTransformedEntity(level);
            if (entity == null) return null;

            identity = new BaseMonsterIdentity(entity, 3); // スキル数3
            if (!level.isClientSide) identity.copyFromPlayerServer(player); // サーバー初期化
        }
        return identity;
    }

    // Client専用
    @Nullable
    public BaseMonsterIdentity getIdentity() {
        return identity;
    }

    // --- 変身Entity取得 ---
    @Nullable
    public BaseMonsterEntity getTransformedEntity(@Nullable Level level) {
        if (!isTransformed || transformedMobId == null) return null;
        if (transformedEntity != null) return transformedEntity;

        if (level == null) return null;
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;

        transformedEntity = type.create(level);
        return transformedEntity;
    }

    // --- 変身開始 ---
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;
        isTransformed = true;
        transformedMobId = mobId;
        Level level = player.getCommandSenderWorld();

        if (!level.isClientSide) {
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {
                transformedEntity = (BaseMonsterEntity) type.create(level);
                if (transformedEntity != null) {
                    transformedEntity.setPos(player.getX(), player.getY(), player.getZ());
                    transformedEntity.setYRot(player.getYRot());
                    transformedEntity.setXRot(player.getXRot());
                    transformedEntity.setYHeadRot(player.getYHeadRot());
                    level.addFreshEntity(transformedEntity);
                }
            }
        }

        // Identityも生成して初期状態同期
        getIdentity(level, player);

        syncToClient(player);
    }

    // --- 変身終了 ---
    public void stopTransformation(Player player) {
        if (!isTransformed) return;
        isTransformed = false;

        if (transformedEntity != null) transformedEntity.discard();
        transformedEntity = null;
        identity = null;
        transformedMobId = null;

        syncToClient(player);
    }

    // --- NBT保存 ---
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("transformedMobId", transformedMobId.toString());
        return tag;
    }

    // --- NBT復元 ---
    public void deserializeNBT(CompoundTag tag) {
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("transformedMobId") ? new ResourceLocation(tag.getString("transformedMobId")) : null;
    }
}
