package com.mimic.monstermod.util;

import com.mimic.monstermod.capability.PlayerTransformation;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.IdentityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MonsterTransformUtil {

    // ================================
    // UUID ごとの HPMap保存用
    // ================================
    private static final Map<UUID, Double> PLAYER_HP_MAP = new HashMap<>();
    // Identity の種類ごとに HP を保存
    private static final Map<UUID, Map<String, Double>> IDENTITY_HP_MAP = new HashMap<>();

    // ================================
    // Player / Identity HP 取得・設定
    // ================================
    public static double getPlayerHP(Player player) {
        return PLAYER_HP_MAP.getOrDefault(player.getUUID(), (double) player.getMaxHealth());
    }

    public static void setPlayerHP(Player player, double hp) {
        PLAYER_HP_MAP.put(player.getUUID(), hp);
        player.setHealth((float) hp);
    }


    // Identity の種類ごとのHP取得（変身中のEntity名を使う前提）
    public static double getIdentityHP(Player player, String identityName) {
        Map<String, Double> map = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());
        return map.getOrDefault(identityName, getIdentityMaxHP(player));
    }


    // Identity HP 設定
    public static void setIdentityHP(Player player, String identityId, double hp) {
        Map<String, Double> map = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());
        map.put(identityId, hp);
        IDENTITY_HP_MAP.put(player.getUUID(), map);

        PlayerTransformation transformation = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && transformation.getEntity() != null &&
                transformation.getIdentity() != null &&
                transformation.getIdentity().getId().equals(identityId)) {
            transformation.getEntity().setHealth((float) hp);
        }
    }

    public static double getIdentityMaxHP(Player player) {
        PlayerTransformation transformation = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && transformation.getEntity() != null) {
            BaseMonsterEntity entity = transformation.getEntity();
            if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                return entity.getAttributeValue(Attributes.MAX_HEALTH);
            }
        }
        return player.getMaxHealth();
    }

    // ================================
    // Attribute コピー / リセット
    // ================================
    public static void copyAttributesToDEV(LivingEntity dev, BaseMonsterEntity identityEntity) {
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

        setAttribute(dev, Attributes.MAX_HEALTH, player.getMaxHealth());
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

    // ================================
    // identity player両方のNBT 保存 / 復元
    // ================================
    public static CompoundTag saveHPToNBT(Player player, CompoundTag tag) {
        tag.putDouble("player_hp", getPlayerHP(player));

        Map<String, Double> identityMap = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());
        CompoundTag idTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : identityMap.entrySet()) {
            idTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("identity_hp_map", idTag);

        return tag;
    }

    public static void loadHPFromNBT(Player player, CompoundTag tag) {
        if (tag.contains("player_hp"))
            PLAYER_HP_MAP.put(player.getUUID(), tag.getDouble("player_hp"));

        if (tag.contains("identity_hp_map")) {
            CompoundTag idTag = tag.getCompound("identity_hp_map");
            Map<String, Double> map = new HashMap<>();
            for (String key : idTag.getAllKeys()) {
                map.put(key, idTag.getDouble(key));
            }
            IDENTITY_HP_MAP.put(player.getUUID(), map);
        }
    }
    // =====================================
    // すべて保存 (HP + Attribute)
    // =====================================
    public static void saveAllToNBT(Player player) {
        CompoundTag tag = new CompoundTag(); // ★ 必ず新規作成する

        saveHPToNBT(player, tag);
        saveAttributesToNBT(player, tag);

        player.getPersistentData().put("hp_save", tag);
    }
    // =====================================
    // すべて復元 (HP + Attribute)
    // =====================================
    public static void loadAllFromNBT(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound("hp_save");
        if (tag.isEmpty()) return;

        loadHPFromNBT(player, tag);
        loadAttributesFromNBT(player, tag);
    }
    // =====================================
    // identity→identity変身用
    // =====================================
    public static void saveIdentityHPToNBT(Player player, String identityId) {
        CompoundTag tag = player.getPersistentData().getCompound("hp_save");
        Map<String, Double> identityMap = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());

        CompoundTag idTag = tag.contains("identity_hp_map") ? tag.getCompound("identity_hp_map") : new CompoundTag();
        if (identityMap.containsKey(identityId)) {
            idTag.putDouble(identityId, identityMap.get(identityId));
        }
        tag.put("identity_hp_map", idTag);

        player.getPersistentData().put("hp_save", tag);
    }
    // =====================================
    // player→identity変身用
    // =====================================
    public static void savePlayerHPToNBT(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound("hp_save");
        tag.putDouble("player_hp", getPlayerHP(player));
        player.getPersistentData().put("hp_save", tag);
    }


    // ================================
    // HP リセット（死亡時・コマンド）
    // ================================
    public static void resetPlayerHP(Player player) {
        double maxHP = player.getMaxHealth();
        PLAYER_HP_MAP.put(player.getUUID(), maxHP);
        player.setHealth((float) maxHP);
    }

    public static void resetIdentityHP(Player player) {
        Map<String, Double> updatedMap = new HashMap<>();

        // IdentityType の全 ID をループ
        for (ResourceLocation id : IdentityType.ID_MAP.keySet()) {
            // Entity は null でも生成可能
            BaseMonsterIdentity identity = IdentityType.createIdentity(id, null);
            if (identity != null) {
                BaseMonsterEntity entity = identity.getEntity();
                if (entity != null && entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double maxHP = entity.getAttributeValue(Attributes.MAX_HEALTH);
                    updatedMap.put(id.toString(), maxHP);
                }
            }
        }

        // プレイヤー UUID に保存
        IDENTITY_HP_MAP.put(player.getUUID(), updatedMap);
    }

    // =====================================
    // Attribute NBT 保存 / 復元
    // =====================================
    public static void saveAttributesToNBT(Player player, CompoundTag tag) {
        PlayerTransformation transformation =
                player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);

        // 変身中 → Identity 属性を保存
        if (transformation != null && transformation.isTransformed() && transformation.getEntity() != null) {
            BaseMonsterEntity id = transformation.getEntity();

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

    public static void loadAttributesFromNBT(Player player, CompoundTag tag) {
        if (!tag.contains("attr_max_health")) return;

        PlayerTransformation transformation =
                player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);

        LivingEntity target = player;

        if (transformation != null && transformation.isTransformed() && transformation.getEntity() != null) {
            target = transformation.getEntity();
        }

        setAttribute(target, Attributes.MAX_HEALTH, tag.getDouble("attr_max_health"));
        setAttribute(target, Attributes.ATTACK_DAMAGE, tag.getDouble("attr_attack"));
        setAttribute(target, Attributes.MOVEMENT_SPEED, tag.getDouble("attr_speed"));
        setAttribute(target, Attributes.ARMOR, tag.getDouble("attr_armor"));
        setAttribute(target, Attributes.KNOCKBACK_RESISTANCE, tag.getDouble("attr_knockback"));

        // HP の再適用
        if (target == player) {
            target.setHealth((float) getPlayerHP(player));
        } else if (transformation != null && transformation.getIdentity() != null) {
            String identityId = transformation.getIdentity().getId();
            target.setHealth((float) getIdentityHP(player, identityId));
        } else {
            // 万が一 Identity が null の場合は最大HPにリセット
            target.setHealth(target.getMaxHealth());
        }
    }
    // ================================
    // ダメージ処理
    // ================================
    public static void damageIdentity(Player player, String identityName, double damage) {
        double hp = getIdentityHP(player, identityName) - damage;
        setIdentityHP(player, identityName, Math.max(0, hp));
    }

    public static void damagePlayer(Player player, double damage) {
        double hp = getPlayerHP(player) - damage;
        setPlayerHP(player, Math.max(0, hp));
    }


}
