package com.mimic.monstermod.capability;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.init.IdentityType;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import com.mimic.monstermod.util.MonsterTransformUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MonsterTransformation {

    private boolean isTransformed = false;
    @Nullable private ResourceLocation transformedMobId = null;
    @Nullable private BaseEntity transformedEntity = null;
    @Nullable private BaseIdentity identity = null;

    public boolean isTransformed() { return isTransformed; }
    public ResourceLocation getMobId() { return transformedMobId; }
    public @Nullable BaseIdentity getIdentity() { return identity; }

// =====================================================================
// 【仕組み：変身中のHP関係】
// 1. 変身した瞬間、その時のPlayerのHP(例:19)を「人間用HP」としてNBTに退避する。
// 2. 変身先(Mimic等)の「保存されていたHP(例:40)」をロードし、最大HP属性を適用してからセットする。
// 3. 変身解除時、今のHP(例:40)を「Identity(Mimic)用HP」として保存。
// 4. 退避させていた「人間用HP(例:19)」を取り出し、属性を人間に戻してから適用する。
public void startTransformation(Player player, ResourceLocation mobId) {
    if (player == null || mobId == null) return;
    MonsterMod.LOGGER.info("[Transform] Starting transformation to: {}", mobId);
    double targetHP = MonsterTransformUtil.getIdentityHP(player, mobId.toString());

    if (targetHP <= 0) {
        player.displayClientMessage(Component.literal("対象のHPが0のため変身できません！"), true);
        return;
    }
    // 現在のHPを保存（死んでいなければ）
    double currentHP = player.getHealth();
    if (currentHP > 0) {
        if (isTransformed && identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), currentHP);
        } else {
            MonsterTransformUtil.setPlayerHP(player, currentHP);
        }
    }

    this.transformedMobId = mobId;
    if (!player.level().isClientSide) {
        this.transformedEntity = ensureEntity(player.level());
        this.identity = ensureIdentity(player.level(), transformedEntity, player);

        if (transformedEntity != null && identity != null) {
            MonsterTransformUtil.copyAttributesToDEV(player, transformedEntity);

            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                        sp.getId(), sp.getAttributes().getSyncableAttributes()
                ));
            }

            double savedHP = MonsterTransformUtil.getIdentityHP(player, identity.getId());
            // 保存HPが0以下なら最大値にする（即死回避）
            if (savedHP <= 0) savedHP = player.getMaxHealth();

            this.isTransformed = true;
            player.setHealth((float) Math.min(savedHP, player.getMaxHealth()));
        }
        player.refreshDimensions();
        syncToAllClients(player);
    }
}
    public void stopTransformation(Player player) {
        if (!isTransformed || player == null) return;

        // 1. [仕組み] 今のモンスターHP(例:40)を保存
        if (identity != null) {
            MonsterTransformUtil.setIdentityHP(player, identity.getId(), player.getHealth());
        }

        // 2. 属性を人間に戻す(MaxHPが20に戻る)
        MonsterTransformUtil.resetAttributesToPlayer(player, player);

        // 3. [重要] 退避していた人間HP(例:19)を復元
        double prevHP = MonsterTransformUtil.getPlayerHP(player);

        if (player instanceof ServerPlayer sp) {
            // 先に人間属性をクライアントに教える
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                    sp.getId(), sp.getAttributes().getSyncableAttributes()
            ));
        }

        // [仕組み] 属性が戻った後にHPをセットすることで、19HPが正しく表示される
        player.setHealth((float) Math.min(prevHP, player.getMaxHealth()));

        isTransformed = false;
        player.refreshDimensions();
        if (!player.level().isClientSide) {
            syncToAllClients(player);
        }
    }

    private BaseIdentity ensureIdentity(Level level, BaseEntity ent, Player player) {
        if (identity != null) return identity;

        if (transformedMobId != null && ent != null) {
            var type = IdentityType.fromId(transformedMobId);
            if (type != null) {
                identity = type.createIdentity(ent);
            }
        }

        // フォールバック（デバッグ・未登録モンスター用）
        if (identity == null && ent != null) {
            identity = new BaseIdentity(ent, 3);
        }

        if (!level.isClientSide && player != null && identity != null) {
            identity.copyFromPlayerServer(player);
        }

        return identity;
    }


    private BaseEntity ensureEntity(Level level) {
        if (transformedEntity != null) return transformedEntity;
        if (transformedMobId == null) return null;

        var type = ModEntitieType.getEntityType(transformedMobId);
        if (type == null) return null;

        transformedEntity = (BaseEntity) type.create(level);
        return transformedEntity;
    }
    public void syncToAllClients(Player player) {
        if (player.level().isClientSide) return;

        // 全員に送るデータは「見た目」と「HP」だけで十分
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        nbt.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        nbt.putDouble("playerHP", MonsterTransformUtil.getPlayerHP(player));

        // IdentityがいるならそのHP、いないなら現在のプレイヤーHPを送る
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
            nbt.putDouble("identityHP", (double) player.getHealth());
        }

        // 3. 自分自身に送信
        ModMessages.sendToPlayer(new S2CTransformSyncPacket(player.getUUID(), nbt), player);
    }
    public void onLoad(Player player) {
        if (player == null) return;

        if (!isTransformed || transformedMobId == null) {
            transformedEntity = null;
            identity = null;

            if (!player.level().isClientSide) player.refreshDimensions();
            return;
        }

        transformedEntity = ensureEntity(player.level());
        identity = ensureIdentity(player.level(), transformedEntity, player);

        if (!player.level().isClientSide) player.refreshDimensions();
    }
    // 修正版 getEntity: null ならその場で生成を試みる (クライアント/サーバー共通)
    public BaseEntity getEntity(Level level) {
        if (transformedEntity == null && isTransformed && transformedMobId != null) {
            transformedEntity = ensureEntity(level);
        }
        return transformedEntity;
    }

    // 既存の getEntity() も念のため残すが、基本は上の level 引数付きを使う
    public @Nullable BaseEntity getEntity() {
        return transformedEntity;
    }

    // MonsterTransformation.java 内の修正

// boolean フィールドと Getter/Setter は削除（GameRuleを参照するため）

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("mobId", transformedMobId == null ? "" : transformedMobId.toString());
        // 設定値の保存は不要になった
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;
        this.isTransformed = tag.getBoolean("isTransformed");
        String idStr = tag.getString("mobId");
        this.transformedMobId = idStr.isEmpty() ? null : new ResourceLocation(idStr);

        this.transformedEntity = null;
        this.identity = null;
    }
}
