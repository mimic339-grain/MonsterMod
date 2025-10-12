package net.mimic.monstermod.entity.monster;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * 完全版 MimicEntity
 * - BaseMonsterEntity を継承
 * - Open/Close状態フラグを保持
 * - プレイヤー移動やキー入力に応じたアニメーションをdecideAnimationで返す
 */
public class MimicEntity extends BaseMonsterEntity {

    // Open/Closeフラグ同期
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
        this.entityData.define(OPEN, false); // デフォルトはClose状態
        this.entityData.define(BITE, false);
    }

    // Open/Close管理
    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }
    public void setOpen(boolean open) {
        this.entityData.set(OPEN, open);
    }

    @Override
    protected String decideAnimation() {
        IMonsterData data = getMonsterData();

        // 1. Skillアニメーション優先
        if (data != null && data.getSkill() != null && !data.getSkill().isEmpty()) {
            return "animation.mimic." + data.getSkill();
        }
        if (isPlayerActivelyMoving()) {
            return isOpen() ? "animation.mimic.open_walk" : "animation.mimic.close_walk";
        }
        return isOpen() ? "animation.mimic.open_idle" : "animation.mimic.idle";
    }

    // -----------------------------
    // 属性作成
    // -----------------------------
    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }
}
