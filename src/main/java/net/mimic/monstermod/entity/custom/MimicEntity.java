package net.mimic.monstermod.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MimicEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean isOpen = false;
    private boolean isBiting = false;

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // 必要ならAI追加 (プレイヤーの変身エンティティなので通常は不要)
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public void bite() {
        if (this.isOpen && !this.isBiting) {
            this.isBiting = true;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            if (isBiting) {
                state.getController().setAnimation(RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE));
                isBiting = false;
                return PlayState.CONTINUE;
            }

            boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1e-6;

            if (isOpen) {
                if (moving) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("openjump"));
                } else {
                    return state.setAndContinue(RawAnimation.begin().then("open", Animation.LoopType.PLAY_ONCE));
                }
            } else {
                if (moving) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("closejump"));
                } else {
                    return state.setAndContinue(RawAnimation.begin().then("close", Animation.LoopType.PLAY_ONCE));
                }
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }
}