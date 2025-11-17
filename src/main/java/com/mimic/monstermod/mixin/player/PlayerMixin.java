package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    // ================================
    // getStandingEyeHeight override
    // ================================
    @Inject(
            method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetStandingEyeHeight(Pose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        self.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    LivingEntity transformed = trans.getEntity();
                    if (transformed != null) {
                        cir.setReturnValue(transformed.getEyeHeight(pose));
                    }
                });
    }

    // ================================
    // getDimensions override
    // ================================
    @Inject(
            method = "getDimensions",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        self.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    LivingEntity transformed = trans.getEntity();
                    if (transformed != null) {
                        cir.setReturnValue(transformed.getDimensions(pose));
                    }
                });
    }

    // ================================
    // LocalPlayer Tick: カメラ高さ・当たり判定を即時更新
    // ================================
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void onPlayerTick(CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer)) return;
        LocalPlayer localPlayer = (LocalPlayer) (Object) this;

        localPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    if (!trans.isTransformed()) return;
                    LivingEntity transformed = trans.getEntity();
                    if (transformed == null) return;

                    // 寸法を変身先に合わせて更新
                    EntityDimensions dims = transformed.getDimensions(localPlayer.getPose());
                    if (!dims.equals(localPlayer.getDimensions(localPlayer.getPose()))) {
                        localPlayer.refreshDimensions(); // 当たり判定・寸法を即時反映
                    }

                    // カメラ高さを即時更新
                    float eyeHeight = transformed.getEyeHeight(localPlayer.getPose());
                    try {
                        java.lang.reflect.Field f = LocalPlayer.class.getDeclaredField("eyeHeight");
                        f.setAccessible(true);
                        f.setFloat(localPlayer, eyeHeight);
                    } catch (Exception ignored) {}
                });
    }
}
