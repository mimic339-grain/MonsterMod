package net.mimic.monstermod.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MimicEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createAnimatableInstanceCache(this);

    // ★変更: EntityDataAccessorはMobの内部状態を同期するために使用。
    // CapabilityからMimicEntityにアニメーション状態を安全に同期できます。
    private static final EntityDataAccessor<String> ANIMATION_STATE =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_BITING =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);


    public MimicEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION_STATE, MimicAnimationState.IDLE.name());
        this.entityData.define(IS_BITING, false);
    }

    // ★変更: Tickメソッドから回転固定ロジックを削除
    // MimicEntityが通常のMobとしてスポーンする場合、この固定は不要です。
    // プレイヤーのダミーとして使う場合、MimicPlayerRendererでプレイヤーの回転が適用されます。
    @Override
    public void tick() {
        super.tick();
        // 必要に応じて、Mimic固有のAIや行動ロジックをここに追加
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoEntity> PlayState predicate(AnimationState<T> tAnimationState) {
        // Capabilityからではなく、MimicEntityの同期されたデータから状態を取得
        MimicAnimationState currentState = MimicAnimationState.valueOf(this.entityData.get(ANIMATION_STATE));
        boolean biting = this.entityData.get(IS_BITING);

        if (biting) {
            tAnimationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.mimic.bite"));
        } else {
            switch (currentState) {
                case OPENING:
                    tAnimationState.getController().setAnimation(RawAnimation.begin().thenPlay("animation.mimic.opening"));
                    // アニメーション終了時にOPEN状態に移行するロジックを検討
                    // これは、アニメーションイベントリスナーで行うのが良いでしょう
                    break;
                case OPEN:
                    tAnimationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.mimic.open"));
                    break;
                case CLOSING:
                    tAnimationState.getController().setAnimation(RawAnimation.begin().thenPlay("animation.mimic.closing"));
                    // アニメーション終了時にIDLE/CLOSED状態に移行するロジックを検討
                    break;
                case CLOSED: // CLOSEDはIDLEと同じアニメーションと仮定
                case IDLE:
                default:
                    tAnimationState.getController().setAnimation(RawAnimation.begin().thenLoop("animation.mimic.idle"));
                    break;
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // NBTデータから同期されたデータを読み込む
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("AnimationState")) {
            try {
                this.setAnimationState(MimicAnimationState.valueOf(pCompound.getString("AnimationState")));
            } catch (IllegalArgumentException e) {
                this.setAnimationState(MimicAnimationState.IDLE);
            }
        }
        this.setBitingState(pCompound.getBoolean("IsBiting"));
    }

    // NBTデータに同期されたデータを書き込む
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString("AnimationState", this.getAnimationState().name());
        pCompound.putBoolean("IsBiting", this.getBitingState());
    }

    // MimicEntityの同期されたアニメーション状態を設定する
    public void setAnimationState(MimicAnimationState state) {
        this.entityData.set(ANIMATION_STATE, state.name());
    }

    // MimicEntityの同期されたアニメーション状態を取得する
    public MimicAnimationState getAnimationState() {
        return MimicAnimationState.valueOf(this.entityData.get(ANIMATION_STATE));
    }

    // MimicEntityの同期された噛みつき状態を設定する
    public void setBitingState(boolean biting) {
        this.entityData.set(IS_BITING, biting);
    }

    // MimicEntityの同期された噛みつき状態を取得する
    public boolean getBitingState() {
        return this.entityData.get(IS_BITING);
    }

    public enum MimicAnimationState {
        IDLE,       // 通常の待機状態
        OPENING,    // 開くアニメーション中
        OPEN,       // 開いた状態
        CLOSING,    // 閉じるアニメーション中
        CLOSED      // 閉じた状態（IDLEと同じ見た目でも良いが状態として区別）
    }
}