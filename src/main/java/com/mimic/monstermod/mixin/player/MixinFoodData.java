package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.variable.CapabilityRegistry; // これを正しくインポート
import com.mimic.monstermod.variable.entity.IPlayerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class MixinFoodData {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(Player player, CallbackInfo ci) {
        // CapabilityRegistry.PLAYER_CAPABILITY を直接参照
        player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.hasState(IPlayerData.STATE_HIDE_FOOD)) {
                // 満腹度と隠し満腹度を固定
                this.foodLevel = 20;
                this.saturationLevel = 5.0f;

                // バニラの処理（お腹を空かせたり回復したり）を停止
                ci.cancel();
            }
        });
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void onAddExhaustion(float exhaustion, CallbackInfo ci) {
        // 疲労蓄積を無効化（お腹が減らなくなる）
        ci.cancel();
    }
}