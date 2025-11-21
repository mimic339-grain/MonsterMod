package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
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
        LivingEntity self = (LivingEntity)(Object)this;
        self.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed() && trans.getEntity() != null) {
                cir.setReturnValue(trans.getEntity().getEyeHeight(pose));
            }
        });
    }

    @Inject(
            method = "getDimensions",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        self.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed() && trans.getEntity() != null) {
                cir.setReturnValue(trans.getEntity().getDimensions(pose));
            }
        });
    }
}
