package com.mimic.monstermod.mixin;

import com.mimic.monstermod.capability.PlayerTransformation;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.impl.ServerInputHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player Tick Mixin
 * - 毎 Tick サーバー側で PlayerTransformation の入力処理を呼び出す
 */
@Mixin(ServerPlayer.class)
public class PlayerTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        LazyOptional<PlayerTransformation> capOpt = player.getCapability(
                PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION
        );

        capOpt.ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            // ServerInputHandler で入力消費
            ServerInputHandler.getInstance().handleInput(player);
        });
    }
}
