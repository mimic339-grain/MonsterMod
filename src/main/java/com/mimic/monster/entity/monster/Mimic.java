package com.mimic.monster.entity.monster;

import com.mimic.monster.entity.MonsterEntity; // あなたの基底クラスを継承
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation.LoopType;
import software.bernie.geckolib.core.object.PlayState;

public class Mimic extends MonsterEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public Mimic(EntityType<? extends MonsterEntity> type, Level worldIn) {
        super(type, worldIn);
    }

    // MonsterEntityのcreateAttributesと合わせて必要属性をセット
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::animationPredicate));
    }

    private <T extends GeoEntity> PlayState animationPredicate(AnimationState<T> event) {
        AnimationController<?> controller = event.getController();

        if (this.getHealth() < this.getMaxHealth() * 0.5) {
            controller.setAnimation(RawAnimation.begin().then("animation.mimic.hurt", LoopType.LOOP));
        } else if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7) {
            controller.setAnimation(RawAnimation.begin().then("animation.mimic.walk", LoopType.LOOP));
        } else {
            controller.setAnimation(RawAnimation.begin().then("animation.mimic.idle", LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
