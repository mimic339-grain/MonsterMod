package com.mimic.monstermod.util;

import com.mimic.monstermod.capability.PlayerTransformation;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Monster変身ユーティリティ 完全版
 * - IdentityHP はサーバ authoritative
 * - Player属性・HPはIdentityからコピー（上書き）
 * - Player素HPおよびIdentityHPを保存
 * - 変身開始／解除時にHP強制上書き
 * - 同期は PlayerTransformation 側で行う
 */
public class MonsterTransformUtil {

    // UUIDごとの IdentityHP管理
    private static final Map<UUID, Map<BaseMonsterIdentity, Float>> identityHPMap = new HashMap<>();

    // 変身前 Player素HP
    private static final Map<UUID, Float> playerPrevHPMap = new HashMap<>();

    // =========================
    // Identity HP
    // =========================
    public static float getIdentityHP(UUID uuid, @Nullable BaseMonsterIdentity identity) {
        if (identity == null) return 20f;

        identityHPMap.putIfAbsent(uuid, new HashMap<>());
        Map<BaseMonsterIdentity, Float> map = identityHPMap.get(uuid);

        return map.computeIfAbsent(identity, id ->
                id.hasCurrentHP() ? id.getCurrentHP() :
                        id.getEntity() != null ? (float) id.getEntity().getAttributeValue(Attributes.MAX_HEALTH) : 20f);
    }

    public static void setIdentityHP(UUID uuid, @Nullable BaseMonsterIdentity identity, float hp) {
        if (identity == null) return;

        identityHPMap.putIfAbsent(uuid, new HashMap<>());
        Map<BaseMonsterIdentity, Float> map = identityHPMap.get(uuid);
        map.put(identity, hp);

        if (identity.hasCurrentHP()) identity.setCurrentHP(hp);
    }

    // =========================
    // Player素HP管理
    // =========================
    public static void setPlayerPrevHP(UUID uuid, float hp) {
        playerPrevHPMap.put(uuid, hp);
    }

    public static float getPlayerPrevHP(UUID uuid) {
        return playerPrevHPMap.getOrDefault(uuid, 20f);
    }

    // =========================
    // 属性コピー / HP強制上書き
    // HP上書きは includeHealth パラメータで制御
    // =========================
    public static void copyAttributesToPlayer(Player player, @Nullable BaseMonsterIdentity identity, boolean includeHealth) {
        if (player == null || identity == null) return;
        LivingEntity entity = identity.getEntity();
        if (entity == null) return;

        copy(player, entity, Attributes.MAX_HEALTH);
        copy(player, entity, Attributes.ATTACK_DAMAGE);
        copy(player, entity, Attributes.MOVEMENT_SPEED);
        copy(player, entity, Attributes.ARMOR);
        copy(player, entity, Attributes.KNOCKBACK_RESISTANCE);

        if (includeHealth) {
            float idHP = getIdentityHP(player.getUUID(), identity);
            double maxHP = entity.getAttributeValue(Attributes.MAX_HEALTH);
            if (player.getAttribute(Attributes.MAX_HEALTH) != null)
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);
            player.setHealth(Math.min(idHP, (float) maxHP));
        }
    }

    private static void copy(Player p, LivingEntity src, Attribute attr) {
        if (p.getAttribute(attr) != null && src.getAttribute(attr) != null)
            p.getAttribute(attr).setBaseValue(src.getAttributeValue(attr));
    }

    public static void resetPlayerAttributes(Player p, boolean includeMaxHealth) {
        if (includeMaxHealth && p.getAttribute(Attributes.MAX_HEALTH) != null) {
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
            p.setHealth(20f);
        }

        set(p, Attributes.ATTACK_DAMAGE, 2.0);
        set(p, Attributes.MOVEMENT_SPEED, 0.1);
        set(p, Attributes.ARMOR, 0.0);
        set(p, Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    private static void set(Player p, Attribute attr, double value) {
        if (p.getAttribute(attr) != null)
            p.getAttribute(attr).setBaseValue(value);
    }

    // =========================
    // 現在変身中 IdentityHP取得
    // =========================
    @Nullable
    public static BaseMonsterIdentity getCurrentIdentity(Player player) {
        return player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .map(PlayerTransformation::getIdentity)
                .orElse(null);
    }

    public static float getCurrentIdentityHP(Player player) {
        BaseMonsterIdentity identity = getCurrentIdentity(player);
        if (identity == null) return player.getHealth();
        return getIdentityHP(player.getUUID(), identity);
    }

    // =========================
    // IdentityHPリセット（死亡・リスポーン時など）
    // =========================
    public static void resetAllIdentityHPs(@NotNull Player player) {
        UUID uuid = player.getUUID();
        Map<BaseMonsterIdentity, Float> map = identityHPMap.computeIfAbsent(uuid, k -> new HashMap<>());
        for (BaseMonsterIdentity identity : map.keySet()) {
            BaseMonsterEntity entity = identity.getEntity();
            if (entity != null) {
                float maxHP = (float) entity.getAttributeValue(Attributes.MAX_HEALTH);
                map.put(identity, maxHP);
                if (identity.hasCurrentHP()) identity.setCurrentHP(maxHP);
            }
        }
    }
}
