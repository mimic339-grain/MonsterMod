package net.mimic.monstermod.entity;

import net.mimic.monstermod.variable.CapabilityRegistry;
import net.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class BaseEntity extends Mob {

    // タイマー・スキル進行用
    protected static final EntityDataAccessor<Integer> TICK;
    protected static final EntityDataAccessor<Integer> SKILLTICK;
    protected static final EntityDataAccessor<String> SKILL;
    protected static final EntityDataAccessor<Integer> SYNCTICK;

    // Owner 管理
    protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNERUUID_ID;

    // 攻撃アニメーション補間
    protected float attackTime;
    protected float prevAttackTime;

    // 攻撃種別・防御中
    protected static final EntityDataAccessor<Integer> ATTACKTYPE;
    protected static final EntityDataAccessor<Integer> RESIST;

    protected BaseEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // ----------------------------
    // SynchedEntityData 初期化
    // ----------------------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TICK, 0);
        this.entityData.define(SKILLTICK, 0);
        this.entityData.define(SKILL, "");
        this.entityData.define(SYNCTICK, 0);
        this.entityData.define(DATA_OWNERUUID_ID, Optional.empty());
        this.entityData.define(ATTACKTYPE, 0);
        this.entityData.define(RESIST, 0);
    }

    // ----------------------------
    // Tick / Skill / Sync
    // ----------------------------
    public int getTick() { return entityData.get(TICK); }
    public void setTick(int val) { entityData.set(TICK, val); }

    public int getSkillTick() { return entityData.get(SKILLTICK); }
    public void setSkillTick(int val) { entityData.set(SKILLTICK, val); }

    public String getSkill() { return entityData.get(SKILL); }
    public void setSkill(String val) { entityData.set(SKILL, val); }

    public int getSyncTick() { return entityData.get(SYNCTICK); }
    public void setSyncTick(int val) { entityData.set(SYNCTICK, val); }

    public int getAttackType() { return entityData.get(ATTACKTYPE); }
    public void setAttackType(int val) { entityData.set(ATTACKTYPE, val); }

    public int getResist() { return entityData.get(RESIST); }
    public void setResist(int val) { entityData.set(RESIST, val); }

    // ----------------------------
    // Owner 管理
    // ----------------------------
    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNERUUID_ID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(uuid));
    }

    @Nullable
    public LivingEntity getOwner() {
        if (getOwnerUUID() != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(getOwnerUUID());
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    public void setOwner(@Nullable Entity entity) {
        if (entity instanceof LivingEntity) setOwnerUUID(entity.getUUID());
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity != null && entity.equals(getOwner());
    }

    // ----------------------------
    // Capability 取得
    // ----------------------------
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }
    // ----------------------------
    // 攻撃アニメーション補間
    // ----------------------------
    public float getAttackProgress(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, prevAttackTime, attackTime);
    }

    // ----------------------------
    // NBT 保存 / 読み込み
    // ----------------------------
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Tick", getTick());
        tag.putInt("SkillTick", getSkillTick());
        tag.putString("Skill", getSkill());
        tag.putInt("SyncTick", getSyncTick());
        if (getOwnerUUID() != null) tag.putUUID("Owner", getOwnerUUID());
        tag.putInt("AttackType", getAttackType());
        tag.putInt("Resist", getResist());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTick(tag.getInt("Tick"));
        setSkillTick(tag.getInt("SkillTick"));
        setSkill(tag.getString("Skill"));
        setSyncTick(tag.getInt("SyncTick"));
        if (tag.hasUUID("Owner")) setOwnerUUID(tag.getUUID("Owner"));
        setAttackType(tag.getInt("AttackType"));
        setResist(tag.getInt("Resist"));
    }

    // ----------------------------
    // 静的初期化
    // ----------------------------
    static {
        TICK = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.INT);
        SKILLTICK = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.INT);
        SKILL = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.STRING);
        SYNCTICK = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.INT);
        DATA_OWNERUUID_ID = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.OPTIONAL_UUID);
        ATTACKTYPE = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.INT);
        RESIST = SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.INT);
    }
}
