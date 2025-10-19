package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public class ClientPlayerEntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickClient(CallbackInfo ci) {
        Player player = (Player)(Object)this;

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    if(trans.isTransformed()) {
                        var identity = trans.getIdentity();
                        if(identity != null) identity.copyFromPlayerClient(player);
                    }
                });
    }
}
