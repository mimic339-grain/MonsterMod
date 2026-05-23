package com.mimic.monstermod.mixin.player;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.gui.components.AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Inject(method = "getMessage", at = @At("RETURN"), cancellable = true)
    private void fixMonsterModButtonText(CallbackInfoReturnable<Component> cir) {
        String text = cir.getReturnValue().getString();
        // ボタンのテキストが「MonsterMod 描画設定を開く: 」で始まっていたら
        if (text.startsWith("MonsterMod 描画設定を開く")) {
            // 強制的にタイトルだけのテキストに差し替える
            cir.setReturnValue(Component.literal("MonsterMod 描画設定を開く"));
        }
    }
}