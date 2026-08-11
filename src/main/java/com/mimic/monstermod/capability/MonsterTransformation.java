package com.mimic.monstermod.capability;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.init.IdentityType;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MonsterTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseEntity transformedEntity = null;
    @Nullable private BaseIdentity identity = null;

    // クラス上部にフィールドを追加
    private CompoundTag deferredIdentityData = null;

    // HPの実体はこのCapabilityインスタンスが直接保持する(1プレイヤー1インスタンス)。
    // これによりserializeNBT/deserializeNBTだけでHPも含めて永続化される。
    private double humanHP = -1; // -1 = 未設定（プレイヤーの最大HPを使う）
    private final Map<String, Double> identityHpMap = new HashMap<>();

    public boolean isTransformed() { return isTransformed; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public @Nullable BaseIdentity getIdentity() { return identity; }

    // ======= HPの読み書き（MonsterTransformUtilから呼ばれる） =======
    public double getHumanHP(double maxHealthFallback) {
        return humanHP >= 0 ? humanHP : maxHealthFallback;
    }

    public void setHumanHP(double hp) {
        this.humanHP = hp;
    }

    public double getIdentityHP(String identityId, double maxHealthFallback) {
        return identityHpMap.getOrDefault(identityId, maxHealthFallback);
    }

    public void setIdentityHP(String identityId, double hp) {
        identityHpMap.put(identityId, hp);
    }

    public void clearIdentityHpMap() {
        identityHpMap.clear();
    }

// =====================================================================
// 【仕組み：変身中のHP関係】
// 1. 変身した瞬間、その時のPlayerのHP(例:19)を「人間用HP」としてNBTに退避する。
// 2. 変身先(Mimic等)の「保存されていたHP(例:40)」をロードし、最大HP属性を適用してからセットする。
// 3. 変身解除時、今のHP(例:40)を「Identity(Mimic)用HP」として保存。
// 4. 退避させていた「人間用HP(例:19)」を取り出し、属性を人間に戻してから適用する。
public void startTransformation(Player player, ResourceLocation mobId) {
    if (player == null || mobId == null) return;
    MonsterMod.LOGGER.info("[Transform] Starting transformation to: {}", mobId);

    // 1. 変身先が存在するかHPデータでチェック
    double targetHP = MonsterTransformUtil.getIdentityHP(player, mobId.toString());
    if (targetHP <= 0) {
        player.displayClientMessage(Component.literal("対象のHPが0のため変身できません！"), true);
        return;
    }

    // 1.5 モンスターとハンターは排他。モンスター化するならハンター状態は解除する。
    // (変身が確定した後に行う。上のHPチェックで失敗した場合にハンターを
    //  解除してしまわないよう、必ずチェックより後に置くこと)
    player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(hunter -> {
        if (hunter.isActive()) {
            hunter.stopHunter(player);
        }
    });

    // 2. [重要] 現在のHPを保存（現在のIdentityがあればそれを使い、なければ人間用HPとして保存）
    double currentHP = player.getHealth();
    if (currentHP > 0) {
        if (isTransformed && identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), currentHP);
        } else {
            MonsterTransformUtil.setPlayerHP(player, currentHP);
        }
    }

    // 3. 変身先が現在と異なる場合、古いIdentityとEntityを破棄
    if (this.transformedMobId != null && !this.transformedMobId.equals(mobId)) {
        this.identity = null;
        this.transformedEntity = null;
    }

    // 4. 新しいIDをセット
    this.transformedMobId = mobId;

    // 5. サーバー側処理
    if (!player.level().isClientSide) {
        // EntityとIdentityを確保（ensureIdentity内で再生成ロジックが動く）
        this.transformedEntity = ensureEntity(player.level());
        this.identity = ensureIdentity(player.level(), transformedEntity, player);

        if (transformedEntity != null && identity != null) {
            // 属性のコピー
            MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);

            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                        sp.getId(), sp.getAttributes().getSyncableAttributes()
                ));
            }

            // 保存されていたHPをロード（なければ最大値）
            double savedHP = MonsterTransformUtil.getIdentityHP(player, identity.getId());
            if (savedHP <= 0) savedHP = player.getMaxHealth();

            this.isTransformed = true;
            player.setHealth((float) Math.min(savedHP, player.getMaxHealth()));
        }
    }

    // 6. 最後にUIと同期
    player.refreshDimensions();
    syncToAllClients(player);
}
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        // 1. [仕組み] インスタンスを破棄する「前」にデータを保存する
        if (identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), player.getHealth());
        }

        // 2. 属性を人間に戻す
        MonsterTransformUtil.resetAttributesToPlayer(player, player);

        // 3. [重要] 退避していた人間HP(例:19)を復元
        double prevHP = MonsterTransformUtil.getPlayerHP(player);

        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                    sp.getId(), sp.getAttributes().getSyncableAttributes()
            ));
        }

        player.setHealth((float) Math.min(prevHP, player.getMaxHealth()));

        // 4. 全ての処理が終わった後にインスタンスを破棄・フラグを折る
        this.identity = null;
        this.transformedEntity = null;
        this.isTransformed = false;

        player.refreshDimensions();
        if (!player.level().isClientSide) {
            syncToAllClients(player);
        }
    }
    private BaseEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;

        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;

        transformedEntity = type.create(level);
        return transformedEntity;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        // キー名を "transformedMobId" に統一
        tag.putString("transformedMobId", transformedMobId == null ? "" : transformedMobId.toString());

        if (isTransformed && identity != null) {
            // Identity内部の状態（クールダウン等）も保存
            tag.put("identity_data", identity.serializeNBT());
        }

        // HP（人間時 / Identityごと）もここに含める。
        // これによりForgeの標準セーブ処理(ICapabilitySerializable)経由で自動的に保存/復元される。
        tag.putDouble("humanHP", humanHP);
        CompoundTag idHpTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : identityHpMap.entrySet()) {
            idHpTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put("identity_hp_map", idHpTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;
        this.isTransformed = tag.getBoolean("isTransformed");

        // キー名を "transformedMobId" に統一
        String idStr = tag.getString("transformedMobId");
        this.transformedMobId = idStr.isEmpty() ? null : new ResourceLocation(idStr);

        this.transformedEntity = null;
        this.identity = null;

        if (tag.contains("identity_data")) {
            this.deferredIdentityData = tag.getCompound("identity_data");
        } else {
            this.deferredIdentityData = null;
        }

        this.humanHP = tag.contains("humanHP") ? tag.getDouble("humanHP") : -1;
        this.identityHpMap.clear();
        if (tag.contains("identity_hp_map")) {
            CompoundTag idHpTag = tag.getCompound("identity_hp_map");
            for (String key : idHpTag.getAllKeys()) {
                this.identityHpMap.put(key, idHpTag.getDouble(key));
            }
        }
    }

    public void syncToAllClients(Player player) {
        if (player.level().isClientSide) return;

        // serializeNBTの結果をベースにする（一貫性の確保）
        CompoundTag nbt = serializeNBT();

        // パケット専用のHPデータを追加
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));
        double currentIdHP = (identity != null) ?
                MonsterTransformUtil.getIdentityHP(player, identity.getId()) : player.getHealth();
        nbt.putDouble("identityHP", currentIdHP);

        ModMessages.sendToAllClients(new S2CTransformSyncPacket(player.getUUID(), nbt));
    }

    public void syncToClient(ServerPlayer player) {
        if (player == null) return;

        // 1. serializeNBT() で変身フラグ、ID、keepTransform、resetHp を一括取得
        CompoundTag nbt = serializeNBT();

        // 2. HP情報（これはserializeNBTに含まれないので手動追加）
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));
        if (identity != null) {
            nbt.putDouble("identityHP", MonsterTransformUtil.getIdentityHP(player, identity.getId()));
        } else {
            // Identityがない場合（変身解除中など）は、現在のHPを暫定で送っておく
            nbt.putDouble("identityHP", player.getHealth());
        }

        // 3. 自分自身に送信
        ModMessages.sendToPlayer(new S2CTransformSyncPacket(player.getUUID(), nbt), player);
    }
    public void onLoad(Player player) {
        if (player == null) return;
        MonsterMod.LOGGER.debug("[Capability] onLoad: isTransformed={}, ID={}", isTransformed, transformedMobId);

        if (!isTransformed || transformedMobId == null) {
            this.transformedEntity = null;
            this.identity = null;
            if (!player.level().isClientSide) player.refreshDimensions();
            return;
        }

        this.transformedEntity = ensureEntity(player.level());
        this.identity = ensureIdentity(player.level(), this.transformedEntity, player);

        if (player.level().isClientSide) {
            player.refreshDimensions();
        }
    }
    private BaseIdentity ensureIdentity(Level level, BaseEntity ent, Player player) {
        // 修正: 既存のidentityが存在しても、IDが異なる場合は古いものを破棄して再作成する
        if (identity != null) {
            if (transformedMobId != null && identity.getId().equals(transformedMobId.toString())) {
                return identity;
            } else {
                // IDが一致しない場合は破棄
                identity = null;
            }
        }

        if (transformedMobId != null && ent != null) {
            var type = IdentityType.fromId(transformedMobId);
            if (type != null) {
                identity = type.createIdentity(ent);
            }
        }

        if (identity == null && ent != null) {
            identity = new BaseIdentity(ent, 3);
        }

        // NBTデータがある場合の復元処理
        if (identity != null && deferredIdentityData != null) {
            identity.deserializeNBT(deferredIdentityData);
            deferredIdentityData = null;
        }

        // サーバー側でプレイヤー情報をコピー
        if (!level.isClientSide && player != null && identity != null) {
            identity.copyFromPlayerServer(player);
        }
        return identity;
    }
    // 修正版：引数なしの getEntity も安全に生成を試みる
    public @Nullable BaseEntity getEntity() {
        if (transformedEntity == null && isTransformed && transformedMobId != null) {
            // 本来はLevelが必要だが、変身中であれば初期化漏れの可能性があるため
            // クライアント側なら Minecraft.getInstance().level から補完する等の対策が必要
            return null;
        }
        return transformedEntity;
    }
    // 修正版 getEntity: null ならその場で生成を試みる (クライアント/サーバー共通)
    public BaseEntity getEntity(Level level) {
        if (transformedEntity == null && isTransformed && transformedMobId != null) {
            transformedEntity = ensureEntity(level);
        }
        return transformedEntity;
    }
}
