package net.mimic.monstermod.entity.client;

import net.mimic.monstermod.entity.custom.MimicEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;

public class ClientMimicEntity extends MimicEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public ClientMimicEntity() {
        super(null, null); // 描画専用なので World は null
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        // 描画だけなら空で OK
    }

    // 座標・回転を手動で設定
    public void setPosAndRot(double x, double y, double z, float yRot, float xRot) {
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
    }

    // アニメーション状態を設定
    public void setAnimationState(MimicAnimationState state) {
        super.setAnimationState(state);
    }
}
