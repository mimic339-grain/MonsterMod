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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MonsterTransformUtil - デバッグログ付き完全版
 *
 * ログ方針:
 * - 重要なメソッド入口で LOGGER.debug を追加
 * - 同一プレイヤー + 同一メソッドが短時間（TICK_MS）に何度も呼ばれた場合は 1 回だけ出力する helper を用意
 */
public class MonsterTransformUtil {

    private static final Logger LOGGER = LogManager.getLogger();
    // 1 ティックを 50ms として近似する（必要なら調整）
    private static final long TICK_MS = 50L;
    // プレイヤーごと・メソッドごとの最後のログ時刻 (ms)
    private static final Map<UUID, Map<String, Long>> LAST_LOG_TIME = new ConcurrentHashMap<>();

    // ================================
    // UUID ごとの HPMap保存用
    // ================================
    private static final Map<UUID, Double> PLAYER_HP_MAP = new HashMap<>();
    // Identity の種類ごとに HP を保存
    private static final Map<UUID, Map<String, Double>> IDENTITY_HP_MAP = new HashMap<>();

    // ================================
    // ログヘルパー
    // ================================
    private static void logOncePerTick(Player player, String key, String message) {
        if (player == null) {
            // player が null の場合は通常ログ
            LOGGER.debug("[MonsterTransformUtil] " + message);
            return;
        }
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        LAST_LOG_TIME.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Map<String, Long> perMethod = LAST_LOG_TIME.get(uuid);
        long last = perMethod.getOrDefault(key, 0L);
        if (now - last >= TICK_MS) {
            LOGGER.debug("[MonsterTransformUtil] " + message);
            perMethod.put(key, now);
        }
    }

    public static void logAlways(String message) {
        LOGGER.debug("[MonsterTransformUtil] " + message);
    }

    // ================================
    // Player / Identity HP 取得・設定
    // ================================
    public static double getPlayerHP(Player player) {
        logOncePerTick(player, "getPlayerHP", "getPlayerHP called for " + (player == null ? "null" : player.getUUID()));
        return PLAYER_HP_MAP.getOrDefault(player.getUUID(), (double) player.getMaxHealth());
    }

    public static void setPlayerHP(Player player, double hp) {
        logOncePerTick(player, "setPlayerHP", String.format("setPlayerHP called for %s: hp=%.2f", player.getUUID(), hp));
        PLAYER_HP_MAP.put(player.getUUID(), hp);
        player.setHealth((float) hp);
    }


    // Identity の種類ごとのHP取得（変身中のEntity名を使う前提）
    public static double getIdentityHP(Player player, String identityName) {
        logOncePerTick(player, "getIdentityHP", String.format("getIdentityHP called for %s identity=%s", player.getUUID(), identityName));
        Map<String, Double> map = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());
        return map.getOrDefault(identityName, getIdentityMaxHP(player));
    }


    // Identity HP 設定
    public static void setIdentityHP(Player player, String identityId, double hp) {
        logOncePerTick(player, "setIdentityHP", String.format("setIdentityHP called for %s identity=%s hp=%.2f", player.getUUID(), identityId, hp));
        Map<String, Double> map = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());
        map.put(identityId, hp);
        IDENTITY_HP_MAP.put(player.getUUID(), map);

        PlayerTransformation transformation = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && transformation.getEntity() != null &&
                transformation.getIdentity() != null &&
                transformation.getIdentity().getId().equals(identityId)) {
            transformation.getEntity().setHealth((float) hp);
            logAlways(String.format("Applied identity hp to transformed entity for player %s identity=%s hp=%.2f", player.getUUID(), identityId, hp));
        }
    }

    public static double getIdentityMaxHP(Player player) {
        logOncePerTick(player, "getIdentityMaxHP", "getIdentityMaxHP called for " + (player == null ? "null" : player.getUUID()));
        PlayerTransformation transformation = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && transformation.getEntity() != null) {
            BaseMonsterEntity entity = transformation.getEntity();
            if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                double v = entity.getAttributeValue(Attributes.MAX_HEALTH);
                logAlways(String.format("getIdentityMaxHP resolved from entity: %s -> %.2f", player.getUUID(), v));
                return v;
            }
        }
        double fallback = player.getMaxHealth();
        logAlways(String.format("getIdentityMaxHP fallback to player max health: %s -> %.2f", player.getUUID(), fallback));
        return fallback;
    }

    // ================================
    // Attribute コピー / リセット
    // ================================
    public static void copyAttributesToDEV(LivingEntity dev, BaseMonsterEntity identityEntity) {
        logOncePerTick(null, "copyAttributesToDEV", String.format("copyAttributesToDEV called dev=%s identityEntity=%s", dev, identityEntity));
        if (identityEntity == null || dev == null) return;

        setAttribute(dev, Attributes.MAX_HEALTH, identityEntity.getAttributeValue(Attributes.MAX_HEALTH));
        setAttribute(dev, Attributes.ATTACK_DAMAGE, identityEntity.getAttributeValue(Attributes.ATTACK_DAMAGE));
        setAttribute(dev, Attributes.MOVEMENT_SPEED, identityEntity.getAttributeValue(Attributes.MOVEMENT_SPEED));
        setAttribute(dev, Attributes.ARMOR, identityEntity.getAttributeValue(Attributes.ARMOR));
        setAttribute(dev, Attributes.KNOCKBACK_RESISTANCE, identityEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

        // HP を IdentityHP に合わせる
        dev.setHealth((float) identityEntity.getHealth());
        logAlways(String.format("copyAttributesToDEV applied attributes and set HP to %.2f on dev=%s", identityEntity.getHealth(), dev));
    }

    public static void resetAttributesToPlayer(Player player) {
        logAlways("[resetAttributesToPlayer] restoring vanilla player attributes for " + player.getUUID());

        setBase(player, Attributes.MAX_HEALTH, 20.0);
        setBase(player, Attributes.ATTACK_DAMAGE, 1.0);
        setBase(player, Attributes.MOVEMENT_SPEED, 0.1);
        setBase(player, Attributes.ARMOR, 0.0);
        setBase(player, Attributes.KNOCKBACK_RESISTANCE, 0.0);

        double hp = PLAYER_HP_MAP.getOrDefault(player.getUUID(), 20.0);
        player.setHealth((float) Math.min(hp, 20.0));
    }

    private static void setBase(Player p, Attribute a, double v) {
        if (p.getAttribute(a) != null)
            p.getAttribute(a).setBaseValue(v);
    }

    private static void setAttribute(LivingEntity entity, Attribute attr, double value) {
        if (entity.getAttribute(attr) != null) {
            Objects.requireNonNull(entity.getAttribute(attr)).setBaseValue(value);
            logAlways(String.format("setAttribute: entity=%s attr=%s value=%.2f", entity, attr, value));
        } else {
            logAlways(String.format("setAttribute: entity=%s attr=%s was null, skip", entity, attr));
        }
    }

    // ================================
    // identity player両方のNBT 保存 / 復元
    // ================================
    public static CompoundTag saveHPToNBT(Player player, CompoundTag tag) {
        logOncePerTick(player, "saveHPToNBT", "saveHPToNBT called for " + (player == null ? "null" : player.getUUID()));
        tag.putDouble("player_hp", getPlayerHP(player));

        Map<String, Double> identityMap = IDENTITY_HP_MAP.getOrDefault(player.getUUID(), new HashMap<>());
        CompoundTag idTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : identityMap.entrySet()) {
            idTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("identity_hp_map", idTag);

        logAlways(String.format("saveHPToNBT saved player_hp=%.2f identity_count=%d for %s",
                getPlayerHP(player), identityMap.size(), player.getUUID()));
        return tag;
    }

    public static void loadHPFromNBT(Player player, CompoundTag tag) {
        logOncePerTick(player, "loadHPFromNBT", "loadHPFromNBT called for " + (player == null ? "null" : player.getUUID()));
        if (tag.contains("player_hp"))
            PLAYER_HP_MAP.put(player.getUUID(), tag.getDouble("player_hp"));

        if (tag.contains("identity_hp_map")) {
            CompoundTag idTag = tag.getCompound("identity_hp_map");
            Map<String, Double> map = new HashMap<>();
            for (String key : idTag.getAllKeys()) {
                map.put(key, idTag.getDouble(key));
            }
            IDENTITY_HP_MAP.put(player.getUUID(), map);
            logAlways(String.format("loadHPFromNBT loaded player=%s identity_count=%d", player.getUUID(), map.size()));
        }
    }
    // ================================
    // HP リセット（死亡時・コマンド）
    // ================================
    public static void resetPlayerHP(Player player) {
        logOncePerTick(player, "resetPlayerHP", "resetPlayerHP called for " + player.getUUID());
        double maxHP = player.getMaxHealth();
        PLAYER_HP_MAP.put(player.getUUID(), maxHP);
        player.setHealth((float) maxHP);
        logAlways(String.format("resetPlayerHP set to max=%.2f for %s", maxHP, player.getUUID()));
    }

    public static void resetIdentityHP(Player player) {
        logOncePerTick(player, "resetIdentityHP", "resetIdentityHP called for " + player.getUUID());
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
                    logAlways(String.format("resetIdentityHP candidate id=%s maxHP=%.2f", id.toString(), maxHP));
                }
            }
        }

        // プレイヤー UUID に保存
        IDENTITY_HP_MAP.put(player.getUUID(), updatedMap);
        logAlways(String.format("resetIdentityHP complete for player=%s identities=%d", player.getUUID(), updatedMap.size()));
    }
    /**
     * プレイヤー死亡時専用の Identity HP リセット関数。
     * IdentityHP が 0 のものだけ MAX_HEALTH に戻す。
     */
    /**
     * プレイヤー死亡時専用の Identity HP リセット関数。
     * IdentityHP が 0 のものだけ MAX_HEALTH に戻す。
     *
     * （MonsterTransformUtil のローカル HP マップに対して処理する）
     */
    public static void resetRespawnIdentityHP(Player player) {
        if (player == null) return;

        logOncePerTick(player, "resetDeadIdentityHP",
                "resetDeadIdentityHP called for " + player.getUUID());

        UUID uuid = player.getUUID();

        // 現在の Identity HP マップを取得
        Map<String, Double> identityHP = IDENTITY_HP_MAP.get(uuid);
        if (identityHP == null || identityHP.isEmpty()) return;

        // 新しいマップにコピー
        Map<String, Double> updatedMap = new HashMap<>(identityHP);

        for (String id : identityHP.keySet()) {
            double hp = identityHP.get(id);

            // HP <= 0 の identity だけリセット
            if (hp <= 0) {
                ResourceLocation rl = new ResourceLocation(id);
                BaseMonsterIdentity identity = IdentityType.createIdentity(rl, null);

                if (identity != null && identity.getEntity() != null) {
                    BaseMonsterEntity entity = identity.getEntity();

                    if (entity.getAttribute(Attributes.MAX_HEALTH) != null) {
                        double maxHP = entity.getAttributeValue(Attributes.MAX_HEALTH);

                        updatedMap.put(id, maxHP);

                        logAlways(String.format(
                                "resetDeadIdentityHP: id=%s HP=0 → maxHP=%.2f",
                                id, maxHP
                        ));
                    }
                }
            }
        }

        // MonsterTransformUtil の IdentityHP マップに保存
        IDENTITY_HP_MAP.put(uuid, updatedMap);

        logAlways("resetDeadIdentityHP complete for player=" + uuid);
    }
    // ================================
    // ダメージ処理
    // ================================
    public static void damageIdentity(Player player, String identityName, double damage) {
        logOncePerTick(player, "damageIdentity", String.format("damageIdentity called for %s identity=%s damage=%.2f", player.getUUID(), identityName, damage));
        double hp = getIdentityHP(player, identityName) - damage;
        setIdentityHP(player, identityName, Math.max(0, hp));
        logAlways(String.format("damageIdentity applied, now identityHP=%.2f for player=%s identity=%s", getIdentityHP(player, identityName), player.getUUID(), identityName));
    }

    public static void damagePlayer(Player player, double damage) {
        logOncePerTick(player, "damagePlayer", String.format("damagePlayer called for %s damage=%.2f", player.getUUID(), damage));
        double hp = getPlayerHP(player) - damage;
        setPlayerHP(player, Math.max(0, hp));
        logAlways(String.format("damagePlayer applied, now playerHP=%.2f for %s", getPlayerHP(player), player.getUUID()));
    }


    public static void saveIdentityHPToNBT(Player player, String identityId) {
        if (player == null || identityId == null) return;

        // まず現在の IdentityHP を Capability に反映
        double currentHP = getIdentityHP(player, identityId);
        PlayerTransformation transformation = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && transformation.getIdentity() != null &&
                transformation.getIdentity().getId().equals(identityId)) {
            transformation.getEntity().setHealth((float) currentHP);
        }

        // NBT に保存
        CompoundTag tag = new CompoundTag();
        saveHPToNBT(player, tag);

        // 実際にプレイヤー NBT に書き込む
        player.getPersistentData().put("monster_transform_hp", tag);

        logAlways(String.format("saveIdentityHPToNBT: saved identityHP=%.2f for %s identity=%s",
                currentHP, player.getUUID(), identityId));
    }

    public static void savePlayerHPToNBT(Player player) {
        if (player == null) return;

        // PlayerHP を Capability に反映
        double currentHP = getPlayerHP(player);
        PlayerTransformation transformation = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation != null && !transformation.isTransformed()) {
            player.setHealth((float) currentHP);
        }

        // NBT に保存
        CompoundTag tag = new CompoundTag();
        saveHPToNBT(player, tag);

        // 実際にプレイヤー NBT に書き込む
        player.getPersistentData().put("monster_transform_hp", tag);

        logAlways(String.format("savePlayerHPToNBT: saved playerHP=%.2f for %s", currentHP, player.getUUID()));
    }
}
