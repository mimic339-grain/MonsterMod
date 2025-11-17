package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTransformationMixin {

    // ------------------------
    // ダメージ処理（Identity変身中）
    // ------------------------
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            // IdentityHP にダメージ反映
            float newHP = Math.max(0f, trans.getCurrentIdentityHP(player) - amount);
            trans.setCurrentIdentityHP(player, newHP); // tick() で Player HP と同期

            cir.setReturnValue(true); // プレイヤー本体へのダメージはキャンセル
        });
    }

    // ------------------------
    // 回復処理（自然回復・アイテム回復制御）
    // ------------------------
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void onHeal(float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            // amount <= 0 は自然回復・誤反映の可能性があるので無視
            if (amount <= 0f) return;

            // IdentityHP に回復を反映（Player本体には反映しない）
            float newHP = trans.getCurrentIdentityHP(player) + amount;
            float maxHP = (float) (trans.getEntity() != null
                    ? trans.getEntity().getAttributeValue(Attributes.MAX_HEALTH)
                    : amount);
            newHP = Math.min(newHP, maxHP);

            trans.setCurrentIdentityHP(player, newHP); // tick() で Player HP 同期

            ci.cancel(); // Player 本体の回復はキャンセル
        });
    }

    // ------------------------
    // tick() 内の自然回復制御は不要
    // PlayerTransformation.tick() 側で IdentityHP と Player HP を同期
    // ------------------------
}
