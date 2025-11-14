package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.identity.impl.MimicIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;

    // ===== Getter / Setter =====
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean transformed) { this.isTransformed = transformed; }
    @Nullable public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(@Nullable ResourceLocation mobId) { this.transformedMobId = mobId; }
    @Nullable public BaseMonsterEntity getEntity() { return transformedEntity; }
    public void setTransformedEntity(@Nullable BaseMonsterEntity entity) { this.transformedEntity = entity; }
    @Nullable public BaseMonsterIdentity getIdentity() { return identity; }

    // ===== Tick =====
    public void tick(Player player) {
        if (!isTransformed) return;

        Level level = player.level();
        BaseMonsterEntity entity = ensureEntity(level);
        if (entity == null) return;

        BaseMonsterIdentity id = ensureIdentity(level, entity, player);

        if (!level.isClientSide) {
            // 攻撃力・移動速度・防御力などの毎tick属性同期
            syncAttributesEveryTick(player, entity);

            // 回転・装備・モンスター内部処理同期
            id.tickServer(player);
            if (entity.getMonsterData() != null) entity.getMonsterData().tick();
            id.copyRotationPoseAndEquip(player);
        } else {
            id.copyFromPlayerClient(player);
        }
    }

    // ===== 変身開始 =====
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;

        isTransformed = true;
        transformedMobId = mobId;
        Level level = player.level();

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
        } else {
            transformedEntity = ensureEntity(level);
        }

        BaseMonsterIdentity id = ensureIdentity(level, transformedEntity, player);
        // HP回復（変身後の最大HPに基づいて回復）
        syncHealth(player, transformedEntity);

        // クライアントに同期
        syncToClient(player);
    }

    // ===== 変身解除 =====
    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        // 属性を元に戻す
        resetPlayerAttributes(player);

        isTransformed = false;

        if (transformedEntity != null) {
            transformedEntity.discard();
            transformedEntity = null;
        }

        identity = null;
        transformedMobId = null;

        syncToClient(player);
    }
    // ===== 毎tick属性同期（HPも含む） =====
    private void syncAttributesEveryTick(Player player, LivingEntity entity) {
        copyAttribute(player, Attributes.MAX_HEALTH, entity);
        copyAttribute(player, Attributes.ATTACK_DAMAGE, entity);
        copyAttribute(player, Attributes.MOVEMENT_SPEED, entity);
        copyAttribute(player, Attributes.ARMOR, entity);
        copyAttribute(player, Attributes.KNOCKBACK_RESISTANCE, entity);
    }

    private void copyAttribute(Player player, Attribute attr, LivingEntity entity) {
        if (player.getAttribute(attr) != null && entity.getAttribute(attr) != null) {
            double current = player.getAttributeValue(attr);
            double target = entity.getAttributeValue(attr);
            if (current != target) { // 値が変わったときだけ更新
                player.getAttribute(attr).setBaseValue(target);
            }
        }
    }
    // ===== 最大HPに基づいたHP回復（変身後に1回だけ回復） =====
    private void syncHealth(Player player, LivingEntity entity) {
        if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
            double maxHealth = entity.getAttributeValue(Attributes.MAX_HEALTH);
            // 最大HPが変わった場合にのみ回復
            if (player.getHealth() < maxHealth) {
                player.setHealth((float) maxHealth); // HPを最大値まで回復
            }
        }
    }
    // ===== 属性リセット =====
    private void resetPlayerAttributes(Player player) {
        setAttribute(player, Attributes.MAX_HEALTH, 20.0);
        player.setHealth(20.0f);

        setAttribute(player, Attributes.ATTACK_DAMAGE, 2.0);
        setAttribute(player, Attributes.MOVEMENT_SPEED, 0.1);
        setAttribute(player, Attributes.ARMOR, 0.0);
        setAttribute(player, Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    private void setAttribute(Player player, Attribute attr, double value) {
        if (player.getAttribute(attr) != null) player.getAttribute(attr).setBaseValue(value);
    }

    // ===== クライアント同期 =====
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    // ===== Entity 確保 =====
    @Nullable
    private BaseMonsterEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;
        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    // ===== Identity 確保 =====
    private BaseMonsterIdentity ensureIdentity(Level level, BaseMonsterEntity entity, Player player) {
        if (identity != null) return identity;

        identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, entity);
        if (identity == null) {
            identity = (entity instanceof MimicEntity) ? new MimicIdentity(entity) : new BaseMonsterIdentity(entity, 3);
        }

        if (!level.isClientSide && player != null) {
            identity.copyFromPlayerServer(player);
        }

        return identity;
    }

    // ===== NBT 保存 / 復元 =====
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) tag.putString("mobId", transformedMobId.toString());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        isTransformed = tag.getBoolean("isTransformed");
        transformedMobId = tag.contains("mobId") ? new ResourceLocation(tag.getString("mobId")) : null;

        if (isTransformed && transformedMobId != null && identity == null) {
            Level level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.isClientSide) {
                BaseMonsterEntity entity = ensureEntity(level);
                if (entity != null) {
                    identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, entity);
                }
            }
        }
    }
}
