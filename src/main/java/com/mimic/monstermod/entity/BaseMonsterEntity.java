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
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * BaseMonsterEntity 完全版（GeckoLib + アニメーションリセット防止）
 */
public abstract class BaseMonsterEntity extends BaseEntity implements GeoEntity {

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static EntityDataAccessor<String> ANIMATION_NAME;

    private String currentAnim = "idle";
    private String lastControllerAnim = null; // predicate 内で最後に再生したアニメーション
    private boolean initialSynced = false;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // -----------------------------
    // SynchedEntityData
    // -----------------------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        if (ANIMATION_NAME == null) {
            ANIMATION_NAME = SynchedEntityData.defineId(this.getClass(), EntityDataSerializers.STRING);
        }
        this.entityData.define(ANIMATION_NAME, "idle");
    }

    // -----------------------------
    // GeckoLib
    // -----------------------------
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        String animName = getAnimation();
        if (animName == null || animName.isEmpty()) return PlayState.STOP;

        AnimationController<?> controller = state.getController();

        // 前回と同じなら再セットせず継続
        if (!animName.equals(lastControllerAnim)) {
            lastControllerAnim = animName;
            controller.setAnimation(RawAnimation.begin().then(animName, Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    // -----------------------------
    // アニメーション管理
    // -----------------------------
    public String getAnimation() {
        return this.entityData.get(ANIMATION_NAME);
    }

    public void setAnimation(String animName) {
        if (animName == null || animName.isEmpty()) return;
        if (!animName.equals(currentAnim)) {
            currentAnim = animName;
            this.entityData.set(ANIMATION_NAME, animName);
            if (!level().isClientSide) syncToClients();
        }
    }

    private void syncToClients() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        IMonsterData data = getMonsterData();
        if (data == null) return;
        S2CMonsterSyncPacket packet = new S2CMonsterSyncPacket(getId(), currentAnim, data.getSkill());
        ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this), packet);
    }

    // -----------------------------
    // Tick
    // -----------------------------
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && !initialSynced) {
            initialSynced = true;
            setAnimation("idle"); // 初期化
        }

        if (level().isClientSide) return;

        IMonsterData data = getMonsterData();
        if (data != null) data.tick();

        // decideAnimation の変化があった場合のみセット
        String decidedAnim = decideAnimation();
        if (decidedAnim != null && !decidedAnim.equals(currentAnim)) {
            setAnimation(decidedAnim);
        }
    }

    // -----------------------------
    // 抽象メソッド
    // -----------------------------
    public abstract String decideAnimation();

    // -----------------------------
    // Capability
    // -----------------------------
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }

    // -----------------------------
    // NBT
    // -----------------------------
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationName", currentAnim);
        IMonsterData data = getMonsterData();
        if (data != null) {
            tag.putString("Skill", data.getSkill());
            tag.putInt("SkillTick", data.getSkillTick());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationName")) currentAnim = tag.getString("AnimationName");
        IMonsterData data = getMonsterData();
        if (data != null) {
            if (tag.contains("Skill")) data.setSkill(tag.getString("Skill"));
            if (tag.contains("SkillTick")) data.setSkillTick(tag.getInt("SkillTick"));
        }
    }

    // -----------------------------
    // 移動状態
    // -----------------------------
    private boolean playerActiveMove = false;
    public void setPlayerActiveMove(boolean moving) { this.playerActiveMove = moving; }
    public boolean isPlayerActivelyMoving() { return this.playerActiveMove; }

    // -----------------------------
    // 属性生成
    // -----------------------------
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
