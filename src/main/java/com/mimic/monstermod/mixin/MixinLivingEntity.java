package com.mimic.monstermod.mixin;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    // lerpTo 補間無効化（変身した Entity のみ）
    @Inject(method = "lerpTo", at = @At("HEAD"), cancellable = true)
    private void onLerp(double x, double y, double z, float yaw, float pitch,
                        int posRotationIncrements, boolean teleport, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof BaseMonsterEntity bme && bme.getMonsterData() != null) {
            self.setPos(x, y, z);
            ci.cancel();
        }
    }

    // deltaMovement 補間無効化（変身した Entity のみ）
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void cancelDeltaMovement(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof BaseMonsterEntity bme && bme.getMonsterData() != null) {
            self.setDeltaMovement(self.getDeltaMovement());
        }
    }
}

