package net.mimic.monstermod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public abstract class BaseMonsterEntity<T extends Enum<T>> extends Mob implements GeoEntity {

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // 独自GRAVITY属性
    public static final Attribute GRAVITY =
            new RangedAttribute("generic.gravity", 1.0D, 0.0D, 10.0D).setSyncable(true);

    private static EntityDataAccessor<String> ANIMATION_STATE;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // ------------------------
    // 抽象メソッド（サブクラスで実装）
    // ------------------------
    protected abstract Class<T> getAnimationStateClass();
    protected abstract T getDefaultAnimationState();
    protected abstract String getAnimationName(T state);
    protected abstract boolean shouldLoop(T state);

    /** サーバ→クライアント用スポーンパケットを派生クラスに実装させる */
    public abstract Packet<ClientGamePacketListener> createSpawnPacket();

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return createSpawnPacket();
    }
    // ------------------------
    // データシンク設定
    // ------------------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        if (ANIMATION_STATE == null) {
            ANIMATION_STATE = SynchedEntityData.defineId(this.getClass(), EntityDataSerializers.STRING);
        }
        this.entityData.define(ANIMATION_STATE, getDefaultAnimationState().name());
    }

    // ------------------------
    // アニメーション操作
    // ------------------------
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

    // ------------------------
    // プレイヤー操作に同期して移動や回転をコピー
    // ------------------------
    public void applyAnimationAndRender(Player player) {
        if (player == null) return;
        this.setDeltaMovement(player.getDeltaMovement());
        this.setYRot(player.getYRot());
        this.yBodyRot = player.yBodyRot;
        this.setXRot(player.getXRot());
        this.yHeadRot = player.yHeadRot;
    }

    // ------------------------
    // String から Enum に変換するユーティリティ
    // ------------------------
    @SuppressWarnings("unchecked")
    public T getAnimationStateEnumFromString(String name) {
        try {
            return Enum.valueOf(getAnimationStateClass(), name);
        } catch (Exception e) {
            return getDefaultAnimationState();
        }
    }

    // ------------------------
    // セーブ / ロード対応
    // ------------------------
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationState")) {
            setAnimationState(getAnimationStateEnumFromString(tag.getString("AnimationState")));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationState", getAnimationState().name());
    }

    // ------------------------
    // デフォルト属性作成（GRAVITY付き）
    // ------------------------
    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage,
            double resistance, double armor, double gravity) {

        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, resistance)
                .add(GRAVITY, gravity);
    }
}
