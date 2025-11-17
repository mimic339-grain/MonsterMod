package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class MonsterFoodRecoveryMixin {
    // 変身中は自然回復をキャンセル
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onFoodTick(Player player, CallbackInfo ci) {
        if (player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .map(trans -> trans.isTransformed())
                .orElse(false)) {
            ci.cancel();
        }
    }
}
