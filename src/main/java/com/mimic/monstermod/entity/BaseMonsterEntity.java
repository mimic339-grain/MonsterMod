package com.mimic.monstermod.entity;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**

 * BaseMonsterEntity 完全版（YSMMOD方式）
 * * モデル生成・描画は Renderer 側に移行
 * * Identity とデータ保持に専念
 * * NBT 保存・復元対応
 * * クライアント側 Tick は Identity に任せる
 */
public abstract class BaseMonsterEntity extends BaseEntity {

    @Nullable
    private BaseMonsterIdentity identity;

    @Nullable
    private CompoundTag pendingIdentityTag;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Nullable
    public BaseMonsterIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(@Nullable BaseMonsterIdentity identity) {
        this.identity = identity;
        if (identity != null) {
            identity.setEntity(this);


            // NBT が残っている場合は反映
            if (pendingIdentityTag != null) {
                try {
                    identity.deserializeNBT(pendingIdentityTag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pendingIdentityTag = null;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();


        if (!level().isClientSide) {
            IMonsterData data = getMonsterData();
            if (data != null) data.tick();
        } else if (identity != null) {
            // クライアント側アニメーションは Identity に任せる
            float delta = 1f / 20f;
            identity.tickClient(delta);
        }


    }

    @Nullable
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        if (identity != null) {
            try {
                tag.put("IdentityData", identity.serializeNBT());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        IMonsterData data = getMonsterData();
        if (data != null) {
            tag.putString("Skill", data.getSkill());
            tag.putInt("SkillTick", data.getSkillTick());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("IdentityData")) {
            CompoundTag identityTag = tag.getCompound("IdentityData");
            if (identity != null) {
                try {
                    identity.deserializeNBT(identityTag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                pendingIdentityTag = identityTag;
            }
        }

        IMonsterData data = getMonsterData();
        if (data != null) {
            if (tag.contains("Skill")) data.setSkill(tag.getString("Skill"));
            if (tag.contains("SkillTick")) data.setSkillTick(tag.getInt("SkillTick"));
        }

    }

    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage, double resistance, double armor, double gravity) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, resistance)
                .add(BaseMonsterEntity.GRAVITY, gravity);
    }

    public static final Attribute GRAVITY = Attributes.ATTACK_KNOCKBACK;
}
