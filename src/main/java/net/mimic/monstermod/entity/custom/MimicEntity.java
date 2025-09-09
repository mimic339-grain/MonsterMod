package net.mimic.monstermod.entity.custom;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class MimicEntity extends BaseMonsterEntity<MimicEntity.MimicAnimationState> {

    public enum MimicAnimationState {
        IDLE, OPENING, OPEN, CLOSING, CLOSED, BITE
    }

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private int animationTick = 0;

    public MimicEntity(EntityType<? extends BaseMonsterEntity<?>> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public void setOpen(boolean open) {
        this.entityData.set(OPEN, open);
    }

    @Override
    protected Class<MimicAnimationState> getAnimationStateClass() {
        return MimicAnimationState.class;
    }

    @Override
    protected MimicAnimationState getDefaultAnimationState() {
        return MimicAnimationState.IDLE;
    }

    @Override
    protected String getAnimationName(MimicAnimationState state) {
        return switch (state) {
            case BITE -> "animation.mimic.bite";
            case OPENING -> "animation.mimic.open";
            case OPEN -> isOpen() ? "animation.mimic.open_idle" : "animation.mimic.openjump";
            case CLOSING -> "animation.mimic.close";
            case CLOSED, IDLE -> isOpen() ? "animation.mimic.closejump" : "animation.mimic.idle";
        };
    }

    @Override
    protected boolean shouldLoop(MimicAnimationState state) {
        return switch (state) {
            case OPEN, CLOSED, IDLE -> true;
            case OPENING, CLOSING, BITE -> false;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            MimicAnimationState animState = getAnimationState();
            String animName = getAnimationName(animState);
            boolean loop = shouldLoop(animState);

            state.getController().setAnimation(loop ? RawAnimation.begin().thenLoop(animName)
                    : RawAnimation.begin().thenPlay(animName));

            if (!loop) {
                animationTick++;
                if (animationTick >= getAnimationDuration(animState)) {
                    switch (animState) {
                        case OPENING -> setAnimationState(MimicAnimationState.OPEN);
                        case CLOSING, BITE -> setAnimationState(MimicAnimationState.CLOSED);
                        default -> {}
                    }
                    animationTick = 0;
                }
            } else {
                animationTick = 0;
            }

            return PlayState.CONTINUE;
        }));
    }

    private int getAnimationDuration(MimicAnimationState state) {
        return switch (state) {
            case BITE -> 20;
            case OPENING, CLOSING -> 15;
            default -> 0;
        };
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("MimicOpen", isOpen());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MimicOpen")) {
            setOpen(tag.getBoolean("MimicOpen"));
        }
    }

    @Override
    public boolean isAnimationLocked() {
        MimicAnimationState state = getAnimationState();
        return state == MimicAnimationState.OPENING ||
                state == MimicAnimationState.CLOSING ||
                state == MimicAnimationState.BITE;
    }

    // MimicEntity 用の属性登録
    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(
                200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D
        );
    }
}
