package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * プレイヤーの変身状態を管理するためのインターフェース。
 * プレイヤーごとに異なる変身情報を保持します。
 */
public interface IPlayerTransformation {

    boolean hasSavedOriginalStats();
    void setOriginalHealth(double hp);
    void setOriginalMaxHealth(double maxHp);
    void setOriginalAttackDamage(double dmg);
    void setOriginalArmor(double armor);
    void setOriginalMoveSpeed(double speed);

    double getOriginalHealth();
    double getOriginalMaxHealth();
    double getOriginalAttackDamage();
    double getOriginalArmor();
    double getOriginalMoveSpeed();

    void clearOriginalStats();

    @Nullable
    Entity getTransformedEntity();

    MimicEntity.MimicAnimationState getAnimationState(ResourceLocation transformedMobId);

    void setTransformedEntity(@Nullable Entity entity);
    // 変身中かどうか
    boolean isTransformed();
    void setTransformed(boolean transformed);

    // 変身中のMonsterID
    ResourceLocation getTransformedMobId();
    void setTransformedMobId(ResourceLocation mobId);

    // Monsterごとの状態取得・更新
    PlayerTransformation.MonsterState getMonsterState(ResourceLocation mobId);
    void setMonsterState(ResourceLocation mobId, PlayerTransformation.MonsterState state);

    // プレイヤーの同期（サーバー→クライアント）
    void syncToClient(Player player);

    // NBT保存・読み込み
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);

    // 変身したプレイヤーのアイデンティティ取得
    @Nullable
    IPlayerIdentity getTransformedIdentity();

    // ノックバック無効フラグ
    boolean isNoKnockback();
    void setNoKnockback(boolean value);

    MimicEntity.MimicAnimationState getBaseState();
    void setBaseState(MimicEntity.MimicAnimationState state);

}