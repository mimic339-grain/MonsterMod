package com.mimic.monstermod.entity;

import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 完全版 BaseMonsterEntity
 * - SynchedEntityData + クライアント予測入力 + GeckoLib対応
 */
public abstract class BaseMonsterEntity extends BaseEntity implements GeoEntity {

    private static final EntityDataAccessor<String> ANIMATION_NAME =
            SynchedEntityData.defineId(BaseMonsterEntity.class, EntityDataSerializers.STRING);

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private boolean initialSynced = false;
    private String lastControllerAnim = null;

    private boolean playerActiveMove = false;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // -----------------------------
    // SynchedEntityData
    // -----------------------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION_NAME, "");
    }

    // -----------------------------
    // GeckoLib
    // -----------------------------
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        String anim = getAnimation();
        if (anim == null || anim.isEmpty()) return PlayState.STOP;
        AnimationController<?> controller = state.getController();
        if (!anim.equals(lastControllerAnim)) {
            lastControllerAnim = anim;
            controller.setAnimation(RawAnimation.begin().then(anim, Animation.LoopType.LOOP));
        }
        return PlayState.CONTINUE;
    }

    // -----------------------------
    // Animation Getter / Setter
    // -----------------------------
    public String getAnimation() { return this.entityData.get(ANIMATION_NAME); }

    public void setAnimation(String anim) {
        if (anim != null && !anim.isEmpty() && !anim.equals(getAnimation())) {
            this.entityData.set(ANIMATION_NAME, anim);
        }
    }

    // -----------------------------
    // クライアント入力ラッパー
    // -----------------------------
    public void moveRelative(float forward, float strafe) {
        super.moveRelative(forward, new net.minecraft.world.phys.Vec3(strafe, 0, forward));
        playerActiveMove = forward != 0 || strafe != 0;
    }
    public void jumpFromGround() { super.jumpFromGround(); }
    public void setSprinting(boolean sprint) { super.setSprinting(sprint); }
    public void setPlayerActiveMove(boolean moving) { this.playerActiveMove = moving; }
    public boolean isPlayerActivelyMoving() { return playerActiveMove; }

    // -----------------------------
    // スキル・攻撃
    // -----------------------------
    public void performAbility(int skillIndex) {
        if (level().isClientSide) return;
        switch (skillIndex) {
            case 0 -> doPrimaryAttack();
            case 1 -> doSecondarySkill();
        }
        // SynchedEntityData でアニメーションが自動同期
    }

    protected void doPrimaryAttack() {}
    protected void doSecondarySkill() {}

    // -----------------------------
    // Tick
    // -----------------------------
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            // 初回同期: デフォルトアニメーションを Entity 名に置き換え
            if (!initialSynced) {
                initialSynced = true;
                ResourceLocation typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
                String typeName = typeKey != null ? typeKey.getPath() : "entity";
                setAnimation("animation." + typeName + ".idle");
            }

            IMonsterData data = getMonsterData();
            if (data != null) data.tick();

            String anim = decideAnimation();
            if (anim != null && !anim.isEmpty()) setAnimation(anim);
        }
    }

    // -----------------------------
    // GeckoLib描画補助
    // -----------------------------
    public void renderOnClient(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());

        poseStack.pushPose();
        poseStack.translate(x - this.getX(), y - this.getY(), z - this.getZ());
        this.getAnimatableInstanceCache(); // GeckoLib内部で呼ばれる
        poseStack.popPose();
    }

    // -----------------------------
    // 抽象メソッド
    // -----------------------------
    public abstract String decideAnimation();

    // -----------------------------
    // Capability
    // -----------------------------
    public IMonsterData getMonsterData() { return CapabilityRegistry.getMonsterData(this); }

    // -----------------------------
    // NBT
    // -----------------------------
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationName", getAnimation());
        IMonsterData data = getMonsterData();
        if (data != null) {
            tag.putString("Skill", data.getSkill());
            tag.putInt("SkillTick", data.getSkillTick());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationName")) setAnimation(tag.getString("AnimationName"));
        IMonsterData data = getMonsterData();
        if (data != null) {
            if (tag.contains("Skill")) data.setSkill(tag.getString("Skill"));
            if (tag.contains("SkillTick")) data.setSkillTick(tag.getInt("SkillTick"));
        }
    }

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

    public static final Attribute GRAVITY = Attributes.ATTACK_KNOCKBACK; // 仮定義
}
