package net.mimic.monstermod.mixin;

import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerCollisionMixin {

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void monstermod_canBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    // 変身中は当たり判定なし
                    cir.setReturnValue(false);
                }
            });
        }
    }
}
