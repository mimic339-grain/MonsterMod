package com.mimic.monstermod.util;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.MonsterTransformation;
import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.init.IdentityType;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class MonsterTransformUtil {

    // ================================
    // Player / Identity HP 取得・設定
    // ================================
    // HPの実体はMonsterTransformation Capabilityインスタンス自身が保持する。
    // (以前はUUIDキーのstatic Mapに保持しており、Capabilityのserialize/deserializeに
    //  含まれず永続化されない・保存タイミングに依存する不具合の原因になっていた)
    public static double getPlayerHP(Player player) {
        return player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(t -> t.getHumanHP(player.getMaxHealth()))
                .orElse((double) player.getMaxHealth());
    }

    // Identity の種類ごとのHP取得（変身中のEntity名を使う前提）
    public static double getIdentityHP(Player player, String identityName) {
        double fallback = getIdentityMaxHP(player);
        return player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(t -> t.getIdentityHP(identityName, fallback))
                .orElse(fallback);
    }

    // Identity HP 設定
    public static void setIdentityHP(Player player, String identityId, double hp) {
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            transformation.setIdentityHP(identityId, hp);

            if (transformation.getEntity() != null &&
                    transformation.getIdentity() != null &&
                    transformation.getHpKey().equals(identityId)) {
                transformation.getEntity().setHealth((float) hp);
            }
        });
    }

    public static double getIdentityMaxHP(Player player) {
        MonsterTransformation transformation = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && transformation.getEntity() != null) {
            BaseEntity entity = transformation.getEntity();
            if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                return entity.getAttributeValue(Attributes.MAX_HEALTH);
            }
        }
        return player.getMaxHealth();
    }

    // ================================
    // Attribute コピー / リセット
    // ================================
    public static void copyAttributesToDEV(LivingEntity dev, BaseEntity identityEntity) {
        if (identityEntity == null || dev == null) return;

        setAttribute(dev, Attributes.MAX_HEALTH, identityEntity.getAttributeValue(Attributes.MAX_HEALTH));
        setAttribute(dev, Attributes.ATTACK_DAMAGE, identityEntity.getAttributeValue(Attributes.ATTACK_DAMAGE));
        setAttribute(dev, Attributes.MOVEMENT_SPEED, identityEntity.getAttributeValue(Attributes.MOVEMENT_SPEED));
        setAttribute(dev, Attributes.ARMOR, identityEntity.getAttributeValue(Attributes.ARMOR));
        setAttribute(dev, Attributes.KNOCKBACK_RESISTANCE, identityEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

        // HP を IdentityHP に合わせる
        dev.setHealth((float) identityEntity.getHealth());
    }

    public static void resetAttributesToPlayer(LivingEntity dev, Player player) {
        if (dev == null) return;

        setAttribute(dev, Attributes.MAX_HEALTH, 20);
        setAttribute(dev, Attributes.ATTACK_DAMAGE, 2.0);
        setAttribute(dev, Attributes.MOVEMENT_SPEED, 0.1);
        setAttribute(dev, Attributes.ARMOR, 0.0);
        setAttribute(dev, Attributes.KNOCKBACK_RESISTANCE, 0.0);

        // HP を PlayerHP に戻す
        double hp = getPlayerHP(player);
        dev.setHealth((float) hp);
    }

    private static void setAttribute(LivingEntity entity, Attribute attr, double value) {
        if (entity.getAttribute(attr) != null) {
            Objects.requireNonNull(entity.getAttribute(attr)).setBaseValue(value);
        }
    }

    // =====================================
    // すべて保存 (Attribute)
    // HP・変身状態自体はMonsterTransformation Capability(ICapabilitySerializable)が
    // Forgeの標準セーブ処理で自動的に保存する。ここではCapabilityに乗らないAttributeの
    // スナップショットのみを別途保存する。
    // =====================================
    public static void saveAllToNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        saveAttributesToNBT(player, tag);

        player.getPersistentData().put("hp_save", tag);
        MonsterMod.LOGGER.info("[Save] Player {}'s attribute snapshot saved.", player.getName().getString());
    }

    // =====================================
    // すべて復元 (Attribute)
    // 変身状態/HPはCapabilityのdeserializeNBTで既に復元済みである前提。
    // ここではAttributeスナップショットの復元のみ行う。
    // =====================================
    public static void loadAllFromNBT(Player player) {
        if (!player.getPersistentData().contains("hp_save")) return;
        CompoundTag tag = player.getPersistentData().getCompound("hp_save");

        loadAttributesFromNBT(player, tag);
    }
    // ================================
    // HP リセット（コマンド用）
    // ================================
    public static void resetPlayerHP(Player player) {
        double maxHP = player.getMaxHealth();
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(t -> t.setHumanHP(maxHP));
        player.setHealth((float) maxHP);
    }

    public static void resetIdentityHP(Player player) {
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            trans.clearIdentityHpMap();

            // IdentityType の全 ID をループ
            for (ResourceLocation id : IdentityType.ID_MAP.keySet()) {
                // Entity は null でも生成可能
                BaseIdentity identity = IdentityType.createIdentity(id, null);
                if (identity != null) {
                    BaseEntity entity = identity.getEntity();
                    if (entity != null && entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                        double maxHP = entity.getAttributeValue(Attributes.MAX_HEALTH);
                        trans.setIdentityHP(id.toString(), maxHP);
                    }
                }
            }
        });
    }
    // =====================================
    // Attribute NBT 保存 / 復元
    // =====================================
    public static void saveAttributesToNBT(Player player, CompoundTag tag) {
        MonsterTransformation transformation =
                player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).orElse(null);

        // 変身中 → Identity 属性を保存
        if (transformation != null && transformation.isTransformed() && transformation.getEntity() != null) {
            BaseEntity id = transformation.getEntity();

            tag.putDouble("attr_max_health", id.getAttributeValue(Attributes.MAX_HEALTH));
            tag.putDouble("attr_attack", id.getAttributeValue(Attributes.ATTACK_DAMAGE));
            tag.putDouble("attr_speed", id.getAttributeValue(Attributes.MOVEMENT_SPEED));
            tag.putDouble("attr_armor", id.getAttributeValue(Attributes.ARMOR));
            tag.putDouble("attr_knockback", id.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        }
        // 変身していない → Player 属性を保存
        else {
            tag.putDouble("attr_max_health", player.getAttributeValue(Attributes.MAX_HEALTH));
            tag.putDouble("attr_attack", player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            tag.putDouble("attr_speed", player.getAttributeValue(Attributes.MOVEMENT_SPEED));
            tag.putDouble("attr_armor", player.getAttributeValue(Attributes.ARMOR));
            tag.putDouble("attr_knockback", player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        }

    }

    public static void setPlayerHP(Player player, double hp) {
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(t -> t.setHumanHP(hp));
        // [バグログ] 属性が20の時に100をセットしようとしていないか確認
        if (hp > player.getMaxHealth()) {
            MonsterMod.LOGGER.warn("Attempted to set HP ({}) higher than MaxHP ({}) for player!", hp, player.getMaxHealth());
        }
        player.setHealth((float) hp);
    }
    public static void applyFullTransformation(Player player, MonsterTransformation trans) {
        if (player == null) return;
        boolean isServer = !player.level().isClientSide;

        // ここでデバッグログを入れると原因が追いやすい
        // MonsterMod.LOGGER.debug("Applying Transform: isTransformed={}, MobId={}", trans.isTransformed(), trans.getMobId());

        if (trans.isTransformed() && trans.getMobId() != null) {
            // 1. 強制的にインスタンスを復元（Entity/Identityの生成）
            trans.onLoad(player);

            BaseEntity idEnt = trans.getEntity(player.level());
            if (idEnt != null) {
                // 2. 属性コピー
                copyAttributesToDEV(player, idEnt);

                // 3. HP同期（保存されている値を適用）
                double currentStoredHP = getIdentityHP(player, trans.getHpKey());

                // サーバー側ならパケットを飛ばしてクライアントの「枠」を広げる
                if (isServer && player instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundUpdateAttributesPacket(
                            sp.getId(), sp.getAttributes().getSyncableAttributes()
                    ));
                }

                // 最大HPが適用された後にセット
                player.setHealth((float) Math.min(currentStoredHP, player.getMaxHealth()));
            }
        } else {
            // 変身していない場合
            resetAttributesToPlayer(player, player);
            if (isServer && player instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundUpdateAttributesPacket(
                        sp.getId(), sp.getAttributes().getSyncableAttributes()
                ));
            }
        }
        player.refreshDimensions();
    }

    // =====================================
// Attribute NBT 保存 / 復元 (修正版)
    public static void loadAttributesFromNBT(Player player, CompoundTag tag) {
        // 属性データがない場合は何もしない
        if (!tag.contains("attr_max_health")) return;

        MonsterTransformation transformation = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).orElse(null);

        // 変身中かどうかでターゲットを決定
        boolean isTransformed = transformation != null && transformation.isTransformed() && transformation.getEntity() != null;
        LivingEntity target = isTransformed ? transformation.getEntity() : player;

        // NBTから値を読み出して適用
        double maxHealth = tag.getDouble("attr_max_health");
        setAttribute(target, Attributes.MAX_HEALTH, maxHealth);
        setAttribute(target, Attributes.ATTACK_DAMAGE, tag.getDouble("attr_attack"));
        setAttribute(target, Attributes.MOVEMENT_SPEED, tag.getDouble("attr_speed"));
        setAttribute(target, Attributes.ARMOR, tag.getDouble("attr_armor"));
        setAttribute(target, Attributes.KNOCKBACK_RESISTANCE, tag.getDouble("attr_knockback"));

        // プレイヤー本体にも同じ値を同期（変身中ならモンスターと同じ値に、変身解除中なら20に戻る）
        if (target != player) {
            setAttribute(player, Attributes.MAX_HEALTH, maxHealth);
            setAttribute(player, Attributes.ATTACK_DAMAGE, tag.getDouble("attr_attack"));
            setAttribute(player, Attributes.MOVEMENT_SPEED, tag.getDouble("attr_speed"));
            setAttribute(player, Attributes.ARMOR, tag.getDouble("attr_armor"));
            setAttribute(player, Attributes.KNOCKBACK_RESISTANCE, tag.getDouble("attr_knockback"));
        }

        // [重要] 現在のHPを設定（属性変更の直後にやらないと最大値でキャップされる）
        if (isTransformed) {
            player.setHealth((float) getIdentityHP(player, transformation.getHpKey()));
        } else {
            player.setHealth((float) getPlayerHP(player));
        }
    }
}
