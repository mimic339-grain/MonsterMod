package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.MonsterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void cancelPlayerShadow(PoseStack p_114458_, MultiBufferSource p_114459_, Entity p_114460_, float p_114461_, float p_114462_, LevelReader p_114463_, float p_114464_, CallbackInfo ci) {
        if (p_114460_ instanceof Player player) {
            boolean transformed = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                    .map(MonsterTransformation::isTransformed)
                    .orElse(false);
            if (transformed) {
                ci.cancel(); // 影描画をスキップ
            }
        }
    }
}