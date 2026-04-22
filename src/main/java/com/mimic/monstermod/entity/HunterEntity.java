package com.mimic.monstermod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class HunterEntity extends BaseEntity {
    private UUID playerUUID;
    private boolean isSlim;

    public HunterEntity(EntityType<? extends BaseEntity> type, Level level) {
        super(type, level);
    }

    public void setPlayerInfo(UUID uuid, boolean slim) {
        this.playerUUID = uuid;
        this.isSlim = slim;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean isSlim() { return isSlim; }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        // ハンター用のアニメーション制御（抜刀待機など）
    }
    // HunterEntity.java に追加
    public static AttributeSupplier.Builder createAttributes() {
        return BaseEntity.createDefaultAttributes(
                20.0D,   // 体力 (HP)
                0.3D,    // 移動速度
                2.0D,    // 攻撃力
                0.0D,    // ノックバック耐性
                0.0D     // アーマー
        );
    }
}