package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationMixinHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * プレイヤー復活時 / クローン時に PlayerTransformation を同期
 */
@Mixin(ServerPlayer.class)
public class PlayerTransformationSyncMixin {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerTransformationMixinHelper.syncTransformation((ServerPlayer) (Object) this);
    }
}
