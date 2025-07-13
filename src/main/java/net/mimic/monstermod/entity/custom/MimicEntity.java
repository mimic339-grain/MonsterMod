package net.mimic.monstermod.entity.custom;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.resources.ResourceLocation;
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

    public enum MimicAnimationState {
        IDLE,
        OPENING,
        OPEN,
        CLOSING,
        CLOSED
    }

    private MimicAnimationState currentAnimationState = MimicAnimationState.IDLE; // 初期状態をIDLEに設定
    private boolean isBiting = false;

    public static final ResourceLocation MODEL_RESOURCE = new ResourceLocation(MonsterMod.MOD_ID, "geo/mimic.geo.json");
    public static final ResourceLocation TEXTURE_RESOURCE = new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    public static final ResourceLocation ANIMATION_RESOURCE = new ResourceLocation(MonsterMod.MOD_ID, "animations/mimic.animations.json");


    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        // エンティティの初期回転を固定
        this.setYRot(0);
        this.setXRot(0);
        this.yHeadRot = 0;
        this.yBodyRot = 0;
    }

    @Override
    protected void registerGoals() {
        // AIゴールはここに何も追加しない（回転を防ぐため）
    }

    // Tickメソッドで回転を明示的に固定する（念のため）
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) { // サーバー側で制御
            this.setYRot(0);
            this.setXRot(0);
            this.yHeadRot = 0;
            this.yBodyRot = 0;
        }
    }


    public void setCurrentAnimationState(MimicAnimationState state) {
        if (this.currentAnimationState != state) {
            this.currentAnimationState = state;
        }
    }

    public MimicAnimationState getCurrentAnimationState() {
        return this.currentAnimationState;
    }

    public void setBiting(boolean biting) {
        this.isBiting = biting;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            if (isBiting) {
                // JSONファイルのアニメーション名に合わせて"bite"に変更。ループタイプはPLAY_ONCE
                state.getController().setAnimation(RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE));
                isBiting = false; // バイトアニメーション後はisBitingをリセット
                return PlayState.CONTINUE;
            }

            boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1e-6;

            switch (currentAnimationState) {
                case IDLE:
                    // JSONファイルのアニメーション名に合わせて"idle"に変更。ループタイプはPLAY_ONCE (hold_on_last_frameに対応)
                    state.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.PLAY_ONCE));
                    break;
                case CLOSED:
                    // JSONファイルのアニメーション名に合わせて"close"に変更。ループタイプはPLAY_ONCE (hold_on_last_frameに対応)
                    state.getController().setAnimation(RawAnimation.begin().then("close", Animation.LoopType.PLAY_ONCE));
                    break;
                case OPENING:
                    // JSONファイルのアニメーション名に合わせて"open"に変更。ループタイプはPLAY_ONCE (hold_on_last_frameに対応)
                    state.getController().setAnimation(RawAnimation.begin().then("open", Animation.LoopType.PLAY_ONCE));
                    break;
                case OPEN:
                    // JSONファイルのアニメーション名に合わせて"openjump" または "open" に変更
                    if (moving) {
                        state.getController().setAnimation(RawAnimation.begin().thenLoop("openjump"));
                    } else {
                        state.getController().setAnimation(RawAnimation.begin().then("open", Animation.LoopType.PLAY_ONCE));
                    }
                    break;
                case CLOSING:
                    // JSONファイルのアニメーション名に合わせて"close"に変更。ループタイプはPLAY_ONCE
                    state.getController().setAnimation(RawAnimation.begin().then("close", Animation.LoopType.PLAY_ONCE));
                    break;
            }
            return PlayState.CONTINUE;
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