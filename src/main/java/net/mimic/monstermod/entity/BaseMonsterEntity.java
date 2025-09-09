package net.mimic.monstermod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 全モンスター共通のベースクラス
 * - アニメーション状態をEnumで管理
 * - セーブ/ロード対応
 */
public abstract class BaseMonsterEntity<T extends Enum<T>> extends Mob implements GeoEntity {

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // 独自GRAVITY属性
    public static final Attribute GRAVITY =
            new RangedAttribute("generic.gravity", 1.0D, 0.0D, 10.0D).setSyncable(true);

    protected abstract Class<T> getAnimationStateClass();
    protected abstract T getDefaultAnimationState();
    protected abstract String getAnimationName(T state);
    protected abstract boolean shouldLoop(T state);

    private static EntityDataAccessor<String> ANIMATION_STATE;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        if (ANIMATION_STATE == null) {
            ANIMATION_STATE = SynchedEntityData.defineId(this.getClass(), EntityDataSerializers.STRING);
        }
        this.entityData.define(ANIMATION_STATE, getDefaultAnimationState().name());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <G extends GeoEntity> PlayState predicate(AnimationState<G> state) {
        T animState = getAnimationState();
        String animationName = getAnimationName(animState);

        if (shouldLoop(animState)) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(animationName));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenPlay(animationName));
        }
        return PlayState.CONTINUE;
    }

    public void setAnimationState(T state) {
        this.entityData.set(ANIMATION_STATE, state.name());
    }

    public T getAnimationState() {
        try {
            return Enum.valueOf(getAnimationStateClass(), this.entityData.get(ANIMATION_STATE));
        } catch (IllegalArgumentException e) {
            return getDefaultAnimationState();
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationState")) {
            try {
                setAnimationState(Enum.valueOf(getAnimationStateClass(), tag.getString("AnimationState")));
            } catch (IllegalArgumentException e) {
                setAnimationState(getDefaultAnimationState());
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationState", getAnimationState().name());
    }

    // デフォルト属性作成（GRAVITY付き）
    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage,
            double resistance, double armor, double gravity) {

        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, resistance)
                .add(GRAVITY, gravity); // ← null防止
    }

    public boolean isAnimationLocked() {
        return false;
    }

    public boolean isHeadless() {
        return true;
    }
}
