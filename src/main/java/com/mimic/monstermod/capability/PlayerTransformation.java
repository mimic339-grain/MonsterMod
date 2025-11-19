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

import java.util.UUID;

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
    @Nullable public ResourceLocation getMobId() { return transformedMobId; }
    @Nullable public BaseMonsterEntity getEntity() { return transformedEntity; }
    @Nullable public BaseMonsterIdentity getIdentity() { return identity; }
    public void markDimensionDirty() { needsDimensionRefresh = true; }
    public boolean consumeDimensionRefresh() { boolean b = needsDimensionRefresh; needsDimensionRefresh = false; return b; }

    // ==============================
    // startTransformation
    //  - 変身前 HP 保存（PlayerPrev or IdentityPrev）
    //  - Entity/Identity 生成
    //  - DEV に Identity の属性をセット
    //  - DEV の HP を IdentityHP に合わせる
    //  - HP/Attributes を NBT に保存
    //  - クライアント同期
    // ==============================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (player == null || mobId == null) return;
        if (isTransformed) {
            // identity->identity の場合：現在の DEV(=identity)HP を現在の identity の map に保存
            UUID uuid = player.getUUID();
            double currentDevHP = player.getHealth();
            MonsterTransformUtil.setIdentityHP(player, Math.max(0.0, currentDevHP));
            // proceed to change identity below
        }

        Level level = player.level();
        UUID uuid = player.getUUID();

        // 保存：変身前（素の）PlayerHP を Map に保持（ここは player.getHealth()）
        double prevPlayerHP = player.getHealth();
        MonsterTransformUtil.setPlayerHP(player, prevPlayerHP);

        // サーバ側で Entity を生成
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

            // Identity 確保
            identity = ensureIdentity(level, transformedEntity, player);

            // DEV (player entity) に Identity の属性を適用（max HP, speed, etc.）
            if (transformedEntity != null) {
                MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);
            }

            // IdentityHP を取得して DEV の HP に合わせる
            double identityHP = MonsterTransformUtil.getIdentityHP(player);
            // setIdentityHP は Map と、もし変身 capability が見つかればその entity の HP も設定する。
            MonsterTransformUtil.setIdentityHP(player, identityHP);
            // 同時に DEV（player）にも HP を反映（copyAttributesToDEV sets dev health to identityEntity.getHealth,
            // but to be safe set player health as well)
            player.setHealth((float)Math.min(identityHP, player.getMaxHealth()));

            // mark transformed state
            isTransformed = true;
            transformedMobId = mobId;

            // NBT 保存（HP と属性）
            saveHPAndAttributesToPlayerPersistent(player);

            // サーバ→クライアント同期（描画用のフラグ等を送る）
            syncToClient(player);
        } else {
            // client side: create client-side entity/identity for rendering (ensure methods will create them)
            transformedEntity = ensureEntity(level);
            identity = ensureIdentity(level, transformedEntity, player);
        }

        markDimensionDirty();
    }

    // ==============================
    // stopTransformation
    //  - IdentityHP を Map に保存
    //  - Player 属性を戻し（MonsterTransformUtil.resetAttributesToPlayer）
    //  - PlayerHP を Map の値から復元
    //  - HP/Attribute を NBT に保存
    //  - Entity を破棄
    //  - クライアント同期
    // ==============================
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        UUID uuid = player.getUUID();

        // ① Identity 現在 HP を保存（DEV の current HP を identity map に保存）
        double currentDevHP = player.getHealth();
        MonsterTransformUtil.setIdentityHP(player, Math.max(0.0, currentDevHP));

        // ② Player の属性を元に戻す（DEV をプレイヤー標準属性へ）
        MonsterTransformUtil.resetAttributesToPlayer(player, player);

        // ③ PlayerHP を Map から復元（素のHPに戻す）
        double prevPlayerHP = MonsterTransformUtil.getPlayerHP(player);
        // safety: clamp to player's max health
        double clampedPrev = Math.min(prevPlayerHP, player.getAttributeValue(Attributes.MAX_HEALTH));
        player.setHealth((float)Math.max(0.0, clampedPrev));

        // ④ NBT に HP/Attribute を保存（永続化）
        saveHPAndAttributesToPlayerPersistent(player);

        // ⑤ エンティティ破棄（サーバ側）
        if (transformedEntity != null && !player.level().isClientSide) {
            transformedEntity.discard();
        }
        transformedEntity = null;
        identity = null;
        transformedMobId = null;
        isTransformed = false;

        // ⑥ 同期
        syncToClient(player);
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
    // syncToClient: send minimal NBT to client for rendering
    // ==============================
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());

        // send authoritative HP values saved in maps (so client can reflect)
        double playerHP = MonsterTransformUtil.getPlayerHP(player);
        double identityHP = MonsterTransformUtil.getIdentityHP(player);
        nbt.putDouble("playerHP", playerHP);
        nbt.putDouble("identityHP", identityHP);

        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    // ==============================
    // serialize/deserialize capability NBT (only transformation flags)
    // Note: full persistent HP/attribute saved to player's persistent data ("hp_save")
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
        // client-side will receive syncToClient data to set HP/attributes for rendering
    }

    // ==============================
    // Helper: persist HP + Attributes to player's persistentData ("hp_save")
    // ==============================
    private void saveHPAndAttributesToPlayerPersistent(Player player) {
        CompoundTag save = player.getPersistentData().getCompound("hp_save");
        // HP via util
        save = MonsterTransformUtil.saveHPToNBT(player, save);
        // attributes: save important attribute base values so they can be restored after login
        saveAttributesToTag(player, save);
        player.getPersistentData().put("hp_save", save);
    }

    private void saveAttributesToTag(Player player, CompoundTag tag) {
        if (player.getAttribute(Attributes.MAX_HEALTH) != null) tag.putDouble("attr_max_health", player.getAttributeValue(Attributes.MAX_HEALTH));
        if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) tag.putDouble("attr_attack", player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null) tag.putDouble("attr_speed", player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        if (player.getAttribute(Attributes.ARMOR) != null) tag.putDouble("attr_armor", player.getAttributeValue(Attributes.ARMOR));
        if (player.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) tag.putDouble("attr_knockback", player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }

    private void loadAttributesFromTag(Player player, CompoundTag tag) {
        if (tag.contains("attr_max_health")) setAttribute(player, Attributes.MAX_HEALTH, tag.getDouble("attr_max_health"));
        if (tag.contains("attr_attack")) setAttribute(player, Attributes.ATTACK_DAMAGE, tag.getDouble("attr_attack"));
        if (tag.contains("attr_speed")) setAttribute(player, Attributes.MOVEMENT_SPEED, tag.getDouble("attr_speed"));
        if (tag.contains("attr_armor")) setAttribute(player, Attributes.ARMOR, tag.getDouble("attr_armor"));
        if (tag.contains("attr_knockback")) setAttribute(player, Attributes.KNOCKBACK_RESISTANCE, tag.getDouble("attr_knockback"));
    }

    private void setAttribute(Player player, net.minecraft.world.entity.ai.attributes.Attribute attr, double value) {
        if (player.getAttribute(attr) != null) player.getAttribute(attr).setBaseValue(value);
    }
}
