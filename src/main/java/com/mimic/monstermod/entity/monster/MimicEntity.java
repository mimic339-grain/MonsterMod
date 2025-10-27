// MimicEntity.java
package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * 完全版 MimicEntity
 * - BaseMonsterEntity を継承
 * - Open/Close 状態と Skill を反映したアニメーションを返す
 */
public class MimicEntity extends BaseMonsterEntity {

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

    public boolean isOpen() { return this.entityData.get(OPEN); }
    public void setOpen(boolean open) { this.entityData.set(OPEN, open); }

    public boolean isBiting() { return this.entityData.get(BITE); }
    public void setBiting(boolean bite) { this.entityData.set(BITE, bite); }

    protected String currentAnim = "animation.mimic.idle";
    @Override
    public String decideAnimation() {
        IMonsterData data = getMonsterData();

        // Skill > Walk > Idle
        if (data != null && data.getSkill() != null && !data.getSkill().isEmpty()) {
            return "animation.mimic." + data.getSkill();
        }
        if (isPlayerActivelyMoving()) {
            return isOpen() ? "animation.mimic.open_walk" : "animation.mimic.close_walk";
        }
        return isOpen() ? "animation.mimic.open_idle" : "animation.mimic.close_walk";
    }

    // -----------------------------
    // 属性作成
    // -----------------------------
    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }
}
