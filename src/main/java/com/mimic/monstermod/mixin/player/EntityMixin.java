package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.util.TransformationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /** BoundingBoxを変身先に丸投げ */
    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<AABB> cir) {
        cir.setReturnValue(TransformationUtil.getBoundingBox((Entity)(Object)this));
    }

    /** Fluid判定を丸投げ */
    @Inject(method = "updateFluidHeightAndDoFluidPushing*", at = @At("HEAD"), cancellable = true)
    private void onUpdateFluidHeightAndDoFluidPushing(CallbackInfo ci) {
        TransformationUtil.updateFluidHeightAndDoFluidPushing((Entity)(Object)this);
        ci.cancel();
    }

    /** isOnFireを変身中は無効化 */
    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    private void onIsOnFire(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TransformationUtil.isOnFire((Entity)(Object)this));
    }
}
