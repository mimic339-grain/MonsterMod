package com.mimic.monstermod.entity;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMonsterSyncPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;

/**
 * BaseMonsterEntity（MimicEntity対応版）
 * GeckoLib Entity の基本機能。
 */
public abstract class BaseMonsterEntity extends BaseEntity implements GeoEntity {

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // ---- 同期データ ----
    private static final EntityDataAccessor<String> CURRENT_ANIMATION =
            SynchedEntityData.defineId(BaseMonsterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_SKILL =
            SynchedEntityData.defineId(BaseMonsterEntity.class, EntityDataSerializers.STRING);

    private boolean initialSynced = false;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // ============================================================== //
    // SynchedEntityData
    // ============================================================== //
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_ANIMATION, "");
        this.entityData.define(CURRENT_SKILL, "");
    }

    public void setCurrentAnimation(String name) {
        this.entityData.set(CURRENT_ANIMATION, name == null ? "" : name);
    }

    public String getCurrentAnimation() {
        return this.entityData.get(CURRENT_ANIMATION);
    }

    public void setSkillState(String skill) {
        this.entityData.set(CURRENT_SKILL, skill == null ? "" : skill);
    }

    public String getSkillState() {
        return this.entityData.get(CURRENT_SKILL);
    }
    // ==============================================================
// プレイヤー移動状態（変身中に使用）
// ==============================================================
    private boolean playerActiveMove = false;

    /**
     * プレイヤーが実際に動いているかどうかを示すフラグ
     * BaseMonsterIdentity から同期される
     */
    public void setPlayerActiveMove(boolean moving) {
        this.playerActiveMove = moving;
    }

    public boolean isPlayerActivelyMoving() {
        return this.playerActiveMove;
    }
    // ============================================================== //
    // GeckoLib
    // ============================================================== //
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public abstract void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers);

    // ============================================================== //
    // Tick & Network
    // ============================================================== //
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && !initialSynced) {
            initialSynced = true;
            syncMonsterState();
        }

        if (!level().isClientSide) {
            IMonsterData data = getMonsterData();
            if (data != null) data.tick();
        }
    }

    public void syncMonsterState() {
        if (level().isClientSide || !(level() instanceof ServerLevel)) return;
        IMonsterData data = getMonsterData();
        String skill = data != null ? data.getSkill() : "";
        String anim = getCurrentAnimation();
        S2CMonsterSyncPacket packet = new S2CMonsterSyncPacket(getId(), anim, skill);
        ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this), packet);
    }

    // ============================================================== //
    // Capability
    // ============================================================== //
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }

    // ============================================================== //
    // NBT
    // ============================================================== //
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationName", getCurrentAnimation());

        IMonsterData data = getMonsterData();
        if (data != null) {
            tag.putString("Skill", data.getSkill());
            tag.putInt("SkillTick", data.getSkillTick());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationName")) setCurrentAnimation(tag.getString("AnimationName"));

        IMonsterData data = getMonsterData();
        if (data != null) {
            if (tag.contains("Skill")) data.setSkill(tag.getString("Skill"));
            if (tag.contains("SkillTick")) data.setSkillTick(tag.getInt("SkillTick"));
        }
    }

    // ============================================================== //
    // 属性生成
    // ============================================================== //
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