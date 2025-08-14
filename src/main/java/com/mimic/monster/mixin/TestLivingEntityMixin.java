package com.mimic.monster.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class TestLivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity instanceof Player player) {
            System.out.println("Mixin Tick Test: プレイヤー " + player.getName().getString());
            player.knockback(0, 0, 0); // ノックバック無効化
        }
    }
}
