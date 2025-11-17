package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.client.Minecraft;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 修正・拡張版 PlayerTransformation
 * - 変身開始時に identityHP が 0 の場合は即座に Map に MaxHP を保存して反映する
 * - 死亡 / リスポーン対策用のメソッドを追加（イベントハンドラから呼ぶ）
 */
public class PlayerTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseMonsterEntity transformedEntity = null;
    @Nullable private BaseMonsterIdentity identity = null;
    private boolean needsDimensionRefresh = false;

    private static final Map<UUID, Float> playerPrevHPMap = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Float>> identityHPMap = new HashMap<>();

    // =========================
    // Getter / Setter
    // =========================
    public boolean isTransformed() { return isTransformed; }
    public void setTransformed(boolean t) { isTransformed = t; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public void setTransformedMobId(ResourceLocation id) { transformedMobId = id; }
    public BaseMonsterEntity getEntity() { return transformedEntity; }
    public BaseMonsterIdentity getIdentity() { return identity; }
    public void markDimensionDirty() { needsDimensionRefresh = true; }
    public boolean consumeDimensionRefresh() { boolean b = needsDimensionRefresh; needsDimensionRefresh = false; return b; }

    // =========================
    // Tick処理
    // =========================
    public void tick(Player player) {
        if (!isTransformed) return;
        Level level = player.level();
        BaseMonsterEntity entity = ensureEntity(level);
        if (entity == null) return;
        BaseMonsterIdentity id = ensureIdentity(level, entity, player);

        if (!level.isClientSide) {
            if (id != null) id.tickServer(player);
            if (entity.getMonsterData() != null) entity.getMonsterData().tick();
            if (id != null) id.copyRotationPoseAndEquip(player);
        } else {
            if (id != null) id.copyFromPlayerClient(player);
        }

        syncPlayerHP(player);
    }

    // =========================
    // 変身開始（完全版）
    // =========================
    public void startTransformation(Player player, ResourceLocation mobId) {
        if (isTransformed) return;

        isTransformed = true;
        transformedMobId = mobId;
        Level level = player.level();

        if (!level.isClientSide) {
            // 変身前のHPを保持
            playerPrevHPMap.put(player.getUUID(), player.getHealth());

            // ------------------------
            // Entity生成
            // ------------------------
            BaseMonsterEntity entity = null;
            var type = ModEntitieType.getEntityType(mobId);
            if (type != null) {
                entity = (BaseMonsterEntity) type.create(level);
                if (entity != null) {
                    entity.moveTo(player.position());
                    entity.setYRot(player.getYRot());
                    entity.setXRot(player.getXRot());
                    entity.setYHeadRot(player.getYHeadRot());
                    level.addFreshEntity(entity);
                }
            }
            transformedEntity = entity;

            // ------------------------
            // 属性コピー（MaxHP含む）
            // ------------------------
            if (entity != null) syncAttributesOnce(player, entity);

            // ------------------------
            // Identity生成と初期化
            // ------------------------
            BaseMonsterIdentity id = ensureIdentity(level, transformedEntity, player);

            // ------------------------
            // IdentityHP マップから取得（初回はMaxHPで初期化）
            // かつ「0ならMaxで上書き」して Map に保存しておく（ここが重要）
            // ------------------------
            float identityHP = getIdentityHP(player.getUUID(), mobId, transformedEntity);
            if (identityHP <= 0f && transformedEntity != null) {
                float maxHP = (float) transformedEntity.getAttributeValue(Attributes.MAX_HEALTH);
                identityHP = maxHP;
                // Map に確実に保存しておく（次回以降0が残らないように）
                setIdentityHP(player.getUUID(), mobId, identityHP);
            }

            // Identity が null なら作る
            if (id == null && transformedEntity != null) {
                id = new BaseMonsterIdentity(transformedEntity, 3);
                identity = id;
            }

            // Identity に currentHP を確実にセット
            if (id != null) id.setCurrentHP(identityHP);

            // Player HP を同期（Max も合わせる）
            if (transformedEntity != null) {
                double newMax = transformedEntity.getAttributeValue(Attributes.MAX_HEALTH);
                if (player.getAttribute(Attributes.MAX_HEALTH) != null)
                    player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
                player.setHealth(Math.min(identityHP, (float) newMax));
            } else {
                player.setHealth(Math.min(identityHP, player.getHealth()));
            }

            // クライアントに Map と Identity の最新を送る（NBT に反映）
            syncToClient(player);

        } else {
            // クライアント側
            transformedEntity = ensureEntity(level);
            ensureIdentity(level, transformedEntity, player);
        }

        markDimensionDirty();
    }

    // =========================
    // 変身終了
    // =========================
    public void stopTransformation(Player player) {
        if (!isTransformed) return;

        // IdentityHP 保存（現状の Identity が持っている値を優先する）
        float currentIdentityHP = (identity != null && identity.hasCurrentHP())
                ? identity.getCurrentHP()
                : getIdentityHP(player.getUUID(), transformedMobId, transformedEntity);
        if (transformedMobId != null) setIdentityHP(player.getUUID(), transformedMobId, currentIdentityHP);

        // プレイヤーのHPと属性を戻す
        float prevHP = playerPrevHPMap.getOrDefault(player.getUUID(), 20f);
        resetAttributes(player, true);
        player.setHealth(prevHP);

        isTransformed = false;
        if (transformedEntity != null) transformedEntity.discard();
        transformedEntity = null;
        identity = null;
        transformedMobId = null;

        syncToClient(player);
        markDimensionDirty();
    }

    // =========================
    // Attributes同期
    // =========================
    public void syncAttributesOnce(Player player, LivingEntity entity) {
        if (player == null || entity == null) return;
        copyAttribute(player, Attributes.MAX_HEALTH, entity);
        copyAttribute(player, Attributes.ATTACK_DAMAGE, entity);
        copyAttribute(player, Attributes.MOVEMENT_SPEED, entity);
        copyAttribute(player, Attributes.ARMOR, entity);
        copyAttribute(player, Attributes.KNOCKBACK_RESISTANCE, entity);
    }

    private void copyAttribute(Player player, Attribute attr, LivingEntity entity) {
        if (player.getAttribute(attr) != null && entity.getAttribute(attr) != null)
            player.getAttribute(attr).setBaseValue(entity.getAttributeValue(attr));
    }

    public void resetAttributes(Player player, boolean includeMaxHealth) {
        if (includeMaxHealth && player.getAttribute(Attributes.MAX_HEALTH) != null) {
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
            player.setHealth(20f);
        }
        setAttribute(player, Attributes.ATTACK_DAMAGE, 2.0);
        setAttribute(player, Attributes.MOVEMENT_SPEED, 0.1);
        setAttribute(player, Attributes.ARMOR, 0.0);
        setAttribute(player, Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    private void setAttribute(Player player, Attribute attr, double value) {
        if (player.getAttribute(attr) != null)
            player.getAttribute(attr).setBaseValue(value);
    }

    // =========================
    // HP同期
    // =========================
    private void syncPlayerHP(Player player) {
        if (!isTransformed || transformedEntity == null) return;

        float idHP = getCurrentIdentityHP(player);
        double maxHP = transformedEntity.getAttributeValue(Attributes.MAX_HEALTH);

        if (player.getAttribute(Attributes.MAX_HEALTH) != null)
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);

        player.setHealth(Math.min(idHP, (float) maxHP));
    }

    // =========================
    // IdentityHP管理
    // =========================
    private float getIdentityHP(UUID uuid, ResourceLocation id, BaseMonsterEntity entity) {
        identityHPMap.putIfAbsent(uuid, new HashMap<>());
        Map<ResourceLocation, Float> map = identityHPMap.get(uuid);

        if (!map.containsKey(id)) {
            float initialHP = (entity != null) ? (float) entity.getAttributeValue(Attributes.MAX_HEALTH) : 5f;
            map.put(id, initialHP);
        }

        return map.get(id);
    }

    public void setIdentityHP(UUID uuid, ResourceLocation id, float hp) {
        identityHPMap.putIfAbsent(uuid, new HashMap<>());
        identityHPMap.get(uuid).put(id, hp);
    }

    /**
     * プレイヤーが死亡したときに呼ぶ（イベントハンドラから呼びます）
     * - 現在変身している identity の HP が 0 以下で残ってしまわないように補正する
     * - 可能なら map 中の 0 以下のエントリをその種族の MaxHP で補正する
     */
    public void onPlayerDeath(ServerPlayer player) {
        if (player == null) return;
        UUID uuid = player.getUUID();

        // 補正：まず現在の変身中 identity（もしあれば）を優先的に補正
        ResourceLocation curId = this.transformedMobId;
        if (curId != null) {
            float cur = getIdentityHP(uuid, curId, transformedEntity);
            if (cur <= 0f) {
                float max = 5f;
                var type = ModEntitieType.getEntityType(curId);
                if (type != null) {
                    BaseMonsterEntity tmp = (BaseMonsterEntity) type.create(player.level());
                    if (tmp != null) max = (float) tmp.getAttributeValue(Attributes.MAX_HEALTH);
                }
                setIdentityHP(uuid, curId, max);
            }
        }

        // 全エントリについても 0 以下のものを補正（念のため）
        ensureNoZeroIdentityHPs(uuid, player.level());
        // そしてクライアントに同期（ServerPlayer がいるなら）
        syncToClient(player);
    }

    /**
     * プレイヤーが（リスポーン等で）クローンされた／リスポーンした際に呼ぶ
     * - identityHPMap 中の 0 以下の値を各 Identity の MaxHP で補正
     * - そのあとクライアント同期
     */
    public void onPlayerRespawn(ServerPlayer player) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        ensureNoZeroIdentityHPs(uuid, player.level());
        syncToClient(player);
    }
    public void copyFrom(PlayerTransformation old, UUID uuid) {
        // 基本状態
        this.isTransformed = old.isTransformed;
        this.transformedMobId = old.transformedMobId;
        this.needsDimensionRefresh = old.needsDimensionRefresh;

        // 変身中の実体はクローン後に再作成するので消す
        this.transformedEntity = null;
        this.identity = null;

        // identityHPMap のデータは static なので UUID が同じなら維持される。
        // しかし、現在の HP が 0 のまま残る事故を防ぐため、0→Max に補正
        if (this.transformedMobId != null) {
            float hp = old.getIdentityHP(uuid, this.transformedMobId, null);
            if (hp <= 0f) {
                float max = 20f;
                var type = ModEntitieType.getEntityType(this.transformedMobId);
                if (type != null) {
                    BaseMonsterEntity tmp = (BaseMonsterEntity) type.create(null);
                    if (tmp != null) max = (float) tmp.getAttributeValue(Attributes.MAX_HEALTH);
                }
                hp = max;
            }
            setIdentityHP(uuid, this.transformedMobId, hp);
        }
    }

    /**
     * 指定プレイヤー (uuid) の identityHPMap 中の 0 以下のエントリを補正する
     */
    public void ensureNoZeroIdentityHPs(UUID uuid, Level level) {
        identityHPMap.putIfAbsent(uuid, new HashMap<>());
        Map<ResourceLocation, Float> map = identityHPMap.get(uuid);

        for (ResourceLocation rid : BaseMonsterIdentityRegistry.getAllIdentityIds()) {
            Float val = map.get(rid);
            if (val == null || val <= 0f) {
                // 該当エンティティを作って MaxHP を取り、保存
                var type = ModEntitieType.getEntityType(rid);
                if (type != null) {
                    BaseMonsterEntity tmp = (BaseMonsterEntity) type.create(level);
                    if (tmp != null) {
                        float max = (float) tmp.getAttributeValue(Attributes.MAX_HEALTH);
                        map.put(rid, max);
                    } else {
                        map.put(rid, 5f);
                    }
                } else {
                    map.put(rid, 5f);
                }
            }
        }
    }

    public float getCurrentIdentityHP(Player player) {
        if (!isTransformed || transformedMobId == null) return player.getHealth();
        return getIdentityHP(player.getUUID(), transformedMobId, transformedEntity);
    }

    public void setCurrentIdentityHP(Player player, float hp) {
        if (!isTransformed || transformedMobId == null) return;
        setIdentityHP(player.getUUID(), transformedMobId, hp);
        if (identity != null) identity.setCurrentHP(hp);
        syncPlayerHP(player);
    }

    // =========================
    // Entity/Identity生成
    // =========================
    private BaseMonsterEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;
        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;
        transformedEntity = (BaseMonsterEntity) type.create(level);
        return transformedEntity;
    }

    private BaseMonsterIdentity ensureIdentity(Level level, BaseMonsterEntity ent, Player player) {
        if (identity != null) return identity;
        identity = BaseMonsterIdentityRegistry.getIdentity(transformedMobId, ent);
        if (identity == null && ent != null) identity = new BaseMonsterIdentity(ent, 3);
        if (!level.isClientSide && player != null && identity != null)
            identity.copyFromPlayerServer(player);
        return identity;
    }

    /**
     * 全ての登録済みIdentityのHPを初期化（MaxHPにリセット）
     */
    public void resetAllIdentityHP(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        Map<ResourceLocation, Float> map = identityHPMap.computeIfAbsent(playerUUID, k -> new HashMap<>());

        for (ResourceLocation rid : BaseMonsterIdentityRegistry.getAllIdentityIds()) {
            var type = ModEntitieType.getEntityType(rid);
            if (type != null) {
                BaseMonsterEntity entity = (BaseMonsterEntity) type.create(player.level());
                if (entity != null) {
                    map.put(rid, (float) entity.getAttributeValue(Attributes.MAX_HEALTH));
                }
            }
        }

        // 変更をクライアントへ通知
        syncToClient(player);
    }

    // =========================
    // クライアント同期
    // =========================
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        CompoundTag nbt = serializeNBT();
        nbt.putFloat("playerHealth", player.getHealth());
        if (identity != null && identity.hasCurrentHP())
            nbt.putFloat("identityHP", identity.getCurrentHP());
        else if (transformedMobId != null) {
            Map<ResourceLocation, Float> map = identityHPMap.get(player.getUUID());
            if (map != null && map.containsKey(transformedMobId))
                nbt.putFloat("identityHP", map.get(transformedMobId));
        }

        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        if (identity != null && identity.hasCurrentHP())
            tag.putFloat("identityHP", identity.getCurrentHP());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        boolean newTrans = tag.getBoolean("isTransformed");
        String idString = tag.getString("mobId");
        float tagHP = tag.contains("identityHP") ? tag.getFloat("identityHP") : -1f;
        Player p = Minecraft.getInstance().player;

        if (!newTrans) {
            isTransformed = false;
            transformedMobId = null;
            transformedEntity = null;
            identity = null;
            if (p != null) resetAttributes(p, true);
            markDimensionDirty();
            return;
        }

        isTransformed = true;
        transformedMobId = idString.isEmpty() ? null : new ResourceLocation(idString);

        if (transformedMobId != null && p != null) {
            Level level = Minecraft.getInstance().level;
            if (level != null && level.isClientSide) {
                transformedEntity = ensureEntity(level);
                identity = ensureIdentity(level, transformedEntity, p);

                // client 側でも Map と Identity を確実に反映（tagHP を優先）
                float maxHP = transformedEntity != null ? (float) transformedEntity.getAttributeValue(Attributes.MAX_HEALTH) : 20f;
                float identityHP = tagHP > 0 ? tagHP : maxHP;
                setIdentityHP(p.getUUID(), transformedMobId, identityHP);
                if (identity != null) identity.setCurrentHP(identityHP);

                // Player 側に Max と HP を反映
                if (transformedEntity != null && p.getAttribute(Attributes.MAX_HEALTH) != null) {
                    p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);
                    p.setHealth(Math.min(identityHP, maxHP));
                }

                syncAttributesOnce(p, transformedEntity);
            }
        }
    }
}
