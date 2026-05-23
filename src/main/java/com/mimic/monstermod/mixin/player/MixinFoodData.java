package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FoodData.class)
public abstract class MixinFoodData {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private int tickTimer;

    @Unique private boolean monstermod$cacheHidden = false;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(Player player, CallbackInfo ci) {
        player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
            this.monstermod$cacheHidden = cap.hasState(IPlayerData.STATE_HIDE_FOOD);
        });

        if (this.monstermod$cacheHidden) {
            this.foodLevel = 20;
            this.saturationLevel = 5.0f;
            this.tickTimer = 0;
            ci.cancel();
        }
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void onAddExhaustion(float exhaustion, CallbackInfo ci) {
        if (this.monstermod$cacheHidden) ci.cancel();
    }
}