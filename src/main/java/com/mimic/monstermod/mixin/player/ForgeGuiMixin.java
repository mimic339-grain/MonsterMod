package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.MonsterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(value = ForgeGui.class, remap = false)
public class ForgeGuiMixin {

    @Inject(method = "renderHealth", at = @At("HEAD"), cancellable = true)
    private void cancelHealth(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(MonsterTransformation::isTransformed).orElse(false)) {

            // Vanilla HP 描画をキャンセル
            ci.cancel();

            // leftHeight を減らしてアーマーバーを上げる
            try {
                Field leftHeightField = ForgeGui.class.getDeclaredField("leftHeight");
                leftHeightField.setAccessible(true);
                int leftHeight = leftHeightField.getInt(this);
                leftHeightField.setInt(this, leftHeight + 10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}