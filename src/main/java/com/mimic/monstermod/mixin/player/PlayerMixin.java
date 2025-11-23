package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.util.TransformationUtil;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(
            method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetStandingEyeHeight(Pose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(TransformationUtil.getEyeHeight((Player)(Object)this, pose));
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        cir.setReturnValue(TransformationUtil.getDimensions((Player)(Object)this, pose));
    }

}
