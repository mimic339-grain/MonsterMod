package net.mimic.monstermod.mixin;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.common.capabilities.IMorphCapability;
import net.mimic.monstermod.common.capabilities.MorphCapabilityAttacher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;tick()V")
    )
    private void monstermod_onTick(CallbackInfo ci) {
        // このMixinはForgeのPlayerLoggedInEventで代替可能なため、
        // 現時点では特別な処理は記述していません。
        // もしtick毎に変身状態を同期する必要がある場合はここにロジックを追加してください。
    }
}