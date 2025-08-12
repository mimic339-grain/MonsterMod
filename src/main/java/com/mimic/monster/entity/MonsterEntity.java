package com.mimic.monster.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.Optional;
import java.util.UUID;

public class MonsterEntity extends Monster implements GeoEntity {

    public static final EntityDataAccessor<Float> SCALE;//大きさ
    public static final EntityDataAccessor<Integer> TICK;//汎用的なカウンターやタイマーとして使う
    public static final EntityDataAccessor<Integer> SKILLTICK;//スキルの発動やクールダウン用タイマー
    public static final EntityDataAccessor<String> SKILL;// 現在使っているスキルの名前
    public static final EntityDataAccessor<Integer> SYNCTICK;//クライアントとの同期用タイマー
    protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNERUUID_ID;//: このモンスターを所有するプレイヤーやエンティティのUUID
    public static final EntityDataAccessor<Integer> PUNCHTYPE;// 攻撃の種類
    public static final EntityDataAccessor<Integer> RESISTANCE;//抵抗値など
    public static final EntityDataAccessor<Boolean> ISTARGET;//現在ターゲットかどうか
    public static final EntityDataAccessor<Boolean> GUARD;//ガード状態かどうか
    public static final EntityDataAccessor<Boolean> PRESS;//押しっぱなしなどの操作状態

    //animationのキャッシュとコントローラー
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public float attackTime;
    public float prevAttackTime;
    //Entityの初期化
    protected MonsterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // アニメーションコントローラの登録(空実装)
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    //entityDataの各フィールドの値を読み書き
    public String getSkill() {
        return this.entityData.get(SKILL);
    }

    public void setSkill(String value) {
        this.entityData.set(SKILL, value);
    }

    public int getTick() {
        return this.entityData.get(TICK);
    }

    public void setTick(int value) {
        this.entityData.set(TICK, value);
    }

    public int getSyncTick() {
        return this.entityData.get(SYNCTICK);
    }

    public void setSyncTick(int value) {
        this.entityData.set(SYNCTICK, value);
    }

    public int getSkillTick() {
        return this.entityData.get(SKILLTICK);
    }

    public void setSkillTick(int value) {
        this.entityData.set(SKILLTICK, value);
    }

    public boolean isGuard() {
        return this.entityData.get(GUARD);
    }

    public void setGuard(boolean value) {
        this.entityData.set(GUARD, value);
    }

    public boolean isPress() {
        return this.entityData.get(PRESS);
    }

    public void setPress(boolean value) {
        this.entityData.set(PRESS, value);
    }

    public int getPunchType() {
        return this.entityData.get(PUNCHTYPE);
    }

    public void setPunchType(int value) {
        this.entityData.set(PUNCHTYPE, value);
    }

    public boolean isTargetNow() {
        return this.entityData.get(ISTARGET);
    }

    public void setTargetNow(boolean value) {
        this.entityData.set(ISTARGET, value);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float value) {
        this.entityData.set(SCALE, value);
    }

    //以下ペットなどのオーナー管理（将来用）
    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNERUUID_ID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(uuid));
    }

    @Nullable
    public LivingEntity getOwner() {
        LivingEntity owner = null;
        if (this.getOwnerUUID() != null && this.level() instanceof ServerLevel) {
            owner = (LivingEntity)((ServerLevel)this.level()).getEntity(this.getOwnerUUID());
            return owner;
        } else{
        return null;
    }
        }

    public void setOwner(@Nullable Entity entity) {
        if (entity != null) {
            this.getPersistentData().putUUID("OWNER_UUID", entity.getUUID());
            this.setOwnerUUID(entity.getUUID());
        }
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity == this.getOwner();
    }
    //ここまで

    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    //エンティティの同期データを初期化し登録
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK, 0);
        this.entityData.define(SYNCTICK, 0);
        this.entityData.define(SKILL, "");
        this.entityData.define(SKILLTICK, 0);
        this.entityData.define(DATA_OWNERUUID_ID, Optional.empty());
        this.entityData.define(PUNCHTYPE, 0);
        this.entityData.define(RESISTANCE, 0);
        this.entityData.define(ISTARGET, false);
        this.entityData.define(GUARD, false);
        this.entityData.define(PRESS, false);
        float scale = 1.0F;
        this.entityData.define(SCALE, scale);
    }
    //エンティティの保存データをNBT形式で保存・読み込み
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Monster_Scale", this.getScale());
        compound.putInt("Monster_Tick", this.getTick());
        compound.putInt("Monster_SyncTick", this.getSyncTick());
        compound.putString("Monster_Skill", this.getSkill());
        compound.putInt("Monster_SkillTick", this.getSkillTick());
        compound.putBoolean("Monster_IsTarget", this.isTargetNow());
        compound.putBoolean("Monster_Guard", this.isGuard());
        compound.putInt("Monster_PunchType", this.getPunchType());
        compound.putBoolean("Monster_Press", this.isPress());
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setScale(compound.getFloat("Monster_Scale"));
        this.setTick(compound.getInt("Monster_Tick"));
        this.setSyncTick(compound.getInt("Monster_SyncTick"));
        this.setSkillTick(compound.getInt("Monster_SkillTick"));
        this.setSkill(compound.getString("Monster_Skill"));
        this.setPunchType(compound.getInt("Monster_PunchType"));
        this.setPress(compound.getBoolean("Monster_Press"));
        this.setTargetNow(compound.getBoolean("Monster_IsTarget"));
        this.setGuard(compound.getBoolean("Monster_Guard"));
        UUID uuid = null;
        if (compound.contains("Owner")) {
            uuid = compound.getUUID("Owner");
        } else if (compound.contains("Owner")) {
            String s = compound.getString("Owner");
            // UUID変換処理があればここに
        }
        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    //同期データが更新された時の処理
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SCALE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    //エンティティの大きさを返す
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.getScale() != 1.0F) {
            EntityDimensions base = this.getType().getDimensions();
            float scale = this.getScale();
            return EntityDimensions.scalable(base.width * scale, base.height * scale);
        } else {
            return super.getDimensions(pose);
        }
    }


    static {
        SCALE = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.FLOAT);
        TICK = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.INT);
        SKILLTICK = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.INT);
        SKILL = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.STRING);
        SYNCTICK = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.INT);
        DATA_OWNERUUID_ID = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.OPTIONAL_UUID);
        PUNCHTYPE = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.INT);
        RESISTANCE = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.INT);
        ISTARGET = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.BOOLEAN);
        GUARD = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.BOOLEAN);
        PRESS = SynchedEntityData.defineId(MonsterEntity.class, EntityDataSerializers.BOOLEAN);
    }
}
