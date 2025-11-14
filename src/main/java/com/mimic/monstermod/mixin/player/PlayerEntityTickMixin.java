package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player tick 時に PlayerTransformation.tick() を呼び出す Mixin
 * - サーバー専用
 * - Identity / MonsterData の Tick を統合
 */
@Mixin(Player.class)
public class PlayerEntityTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        Level level = player.getCommandSenderWorld();

        // サーバー専用
        if (level.isClientSide) return;
        if (!(player instanceof ServerPlayer)) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    transformation.tick(player);
                });
    }
}
