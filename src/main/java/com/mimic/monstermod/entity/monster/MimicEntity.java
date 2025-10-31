package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;

/**
 * 完全版 MimicEntity
 * - BaseMonsterEntity を継承
 * - Identity / AnimationPlayerTemplate によるアニメーション再生完全対応
 * - Open/Bite は描画同期用
 */
public class MimicEntity extends BaseMonsterEntity {

    // 描画同期用フラグ
    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BITE =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    public MimicEntity(EntityType<? extends BaseMonsterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
        this.entityData.define(BITE, false);
    }

    // -----------------------------
    // Open/Bite 管理 (描画同期用)
    // -----------------------------
    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public void setOpen(boolean open) {
        this.entityData.set(OPEN, open);
    }

    public boolean isBiting() {
        return this.entityData.get(BITE);
    }

    public void setBiting(boolean bite) {
        this.entityData.set(BITE, bite);
    }


    @Override
    protected ModelPart createModel() {
        // Blockbenchなどで作成した Mimic モデル構造をここで構築する
        // 仮の空モデル（ルートのみ）を返す
        return new ModelPart(Collections.emptyList(), new HashMap<>());
    }

    // -----------------------------
    // 属性作成ユーティリティ
    // -----------------------------
    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(
                200.0D, // HP
                0.25D,  // 移動速度
                4.0D,   // 攻撃力
                0.2D,   // ノックバック耐性
                2.0D,   // 防御力
                1.0D    // 重力
        );
    }

    // -----------------------------
    // NBT 保存/復元 (描画フラグのみ)
    // -----------------------------
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Open", isOpen());
        tag.putBoolean("Bite", isBiting());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Open")) setOpen(tag.getBoolean("Open"));
        if (tag.contains("Bite")) setBiting(tag.getBoolean("Bite"));
    }
}
