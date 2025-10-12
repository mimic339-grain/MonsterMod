package net.mimic.monstermod.capability;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.entity.ModEntitieType;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.server.S2CTransformSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * 汎用版 PlayerTransformation
 * - 特定モンスターに依存しない
 * - mobId から EntityType を取得して変身
 * - 状態変化時のみクライアント同期
 */
public class PlayerTransformation {

    // -----------------------------
    // 元のプレイヤー情報
    // -----------------------------
    private boolean originalStatsSaved = false;
    private double originalHealth, originalMaxHealth, originalAttack, originalArmor, originalSpeed;

    // -----------------------------
    // 変身状態
    // -----------------------------
    private boolean isTransformed = false;
    @Nullable
    private ResourceLocation transformedMobId = null;

    @Nullable
    private BaseMonsterEntity transformedEntity = null;

    // ======================
    // 元のステータス管理
    // ======================
    public boolean hasSavedOriginalStats() { return originalStatsSaved; }
    public void setOriginalHealth(double hp) { originalHealth = hp; originalStatsSaved = true; }
    public void setOriginalMaxHealth(double maxHp) { originalMaxHealth = maxHp; }
    public void setOriginalAttackDamage(double dmg) { originalAttack = dmg; }
    public void setOriginalArmor(double armor) { originalArmor = armor; }
    public void setOriginalMoveSpeed(double speed) { originalSpeed = speed; }
    public double getOriginalHealth() { return originalHealth; }
    public double getOriginalMaxHealth() { return originalMaxHealth; }
    public double getOriginalAttackDamage() { return originalAttack; }
    public double getOriginalArmor() { return originalArmor; }
    public double getOriginalMoveSpeed() { return originalSpeed; }
    public void clearOriginalStats() { originalStatsSaved = false; }

    // ======================
    // 変身管理
    // ======================
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean transformed) { isTransformed = transformed; }

    @Nullable
    public ResourceLocation getTransformedMobId() { return transformedMobId; }
    public void setTransformedMobId(ResourceLocation mobId) { transformedMobId = mobId; }

    /**
     * サーバーまたはクライアント描画用に変身エンティティを取得
     */
    @Nullable
    public BaseMonsterEntity getTransformedEntity(Level level) {
        if (!isTransformed || transformedMobId == null) return null;

        // すでに生成済みならそのまま返す
        if (transformedEntity != null) return transformedEntity;

        // サーバー側はスポーン済みのエンティティを返す
        if (!level.isClientSide) return transformedEntity;

        // クライアント側は描画用に生成
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;

        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    /**
     * 変身開始（サーバー側）
     */
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;

        isTransformed = true;
        transformedMobId = mobId;

        Level level = player.getCommandSenderWorld();
        if (!level.isClientSide) {
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {
                transformedEntity = (BaseMonsterEntity) type.create(level);
                transformedEntity.setPos(player.getX(), player.getY(), player.getZ());
                level.addFreshEntity(transformedEntity);
            }
        }

        syncToClient(player);
    }

    /**
     * 変身解除（サーバー側）
     */
    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        isTransformed = false;

        if (transformedEntity != null) {
            transformedEntity.discard();
            transformedEntity = null;
        }

        transformedMobId = null;
        syncToClient(player);
    }

    // ======================
    // サーバー → クライアント同期
    // ======================
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (transformedMobId == null && !isTransformed) return;

        S2CTransformSyncPacket packet = new S2CTransformSyncPacket(
                player.getUUID(),
                transformedMobId,
                isTransformed
        );
        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), packet);

        MonsterMod.getLogger().debug("[syncToClient] player={} transformed={} mobId={}",
                player.getName().getString(), isTransformed, transformedMobId);
    }

    // ======================
    // NBT 保存 / 読み込み
    // ======================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("transformedMobId", transformedMobId.toString());
        tag.putBoolean("originalStatsSaved", originalStatsSaved);
        tag.putDouble("originalHealth", originalHealth);
        tag.putDouble("originalMaxHealth", originalMaxHealth);
        tag.putDouble("originalAttack", originalAttack);
        tag.putDouble("originalArmor", originalArmor);
        tag.putDouble("originalSpeed", originalSpeed);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("transformedMobId") ? new ResourceLocation(tag.getString("transformedMobId")) : null;

        originalStatsSaved = tag.getBoolean("originalStatsSaved");
        originalHealth = tag.getDouble("originalHealth");
        originalMaxHealth = tag.getDouble("originalMaxHealth");
        originalAttack = tag.getDouble("originalAttack");
        originalArmor = tag.getDouble("originalArmor");
        originalSpeed = tag.getDouble("originalSpeed");

        MonsterMod.getLogger().debug("[deserializeNBT] transformed={} mobId={}", isTransformed, transformedMobId);
    }
}
