package com.mimic.monstermod.entity;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMonsterSyncPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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

public abstract class BaseMonsterEntity extends BaseEntity implements GeoEntity {

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static EntityDataAccessor<String> ANIMATION_NAME;

    /** ローカル用の現在アニメーション */
    protected String currentAnim = "idle";
    private String lastControllerAnim = null;
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
        // サーバー側で mapAnimation を通す前提: 初期値もマッピング済み
        this.entityData.define(ANIMATION_NAME, mapAnimation("idle"));
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
        // サーバーから送られたマッピング済みアニメーション名をそのまま使用
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

    /**
     * サーバー側からアニメーション開始イベントを送信
     */
    public void playAnimationEvent(String animName) {
        if (animName == null || animName.isEmpty()) return;

        currentAnim = animName;
        String mappedAnim = mapAnimation(animName);
        entityData.set(ANIMATION_NAME, mappedAnim);

        if (!level().isClientSide && level() instanceof ServerLevel) {
            S2CMonsterSyncPacket packet = new S2CMonsterSyncPacket(getId(), mappedAnim, null);
            ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this), packet);
        }
    }

    /**
     * サーバー専用: "animation.<entity>.<anim>" に変換
     */
    private String mapAnimation(String anim) {
        if (anim == null || anim.isEmpty()) return "";
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        if (id == null) return anim;
        return "animation." + id.getPath() + "." + anim;
    }

    // -----------------------------
    // Tick
    // -----------------------------
    @Override
    public void tick() {
        super.tick();

        // 初回同期: サーバー → クライアント
        if (!level().isClientSide && !initialSynced) {
            initialSynced = true;
            playAnimationEvent("idle");
        }

        if (level().isClientSide) return;

        // クールダウンやスキルTickのみ
        IMonsterData data = getMonsterData();
        if (data != null) data.tick();
    }

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

    // -----------------------------
    // 抽象メソッド
    // -----------------------------
    public abstract String decideAnimation();
}
