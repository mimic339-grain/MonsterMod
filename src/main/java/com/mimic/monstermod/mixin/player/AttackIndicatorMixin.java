package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class AttackIndicatorMixin {

    @Shadow
    protected int screenWidth;
    @Shadow
    protected int screenHeight;

    @Final
    @Shadow
    protected static ResourceLocation GUI_ICONS_LOCATION;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (HunterTransformation.isHunter(mc.player)) {
            ci.cancel(); // 元の描画をキャンセル

            Options options = mc.options;
            if (options.getCameraType().isFirstPerson()) {
                // バニラと同じ色味にするためブレンド関数設定
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO
                );

                // 十字クロスヘア描画（バニラと同じサイズ・座標）
                int x = (screenWidth - 15) / 2;
                int y = (screenHeight - 15) / 2;
                guiGraphics.blit(GUI_ICONS_LOCATION, x, y, 0, 0, 15, 15);

                // ブレンド関数を元に戻す
                RenderSystem.defaultBlendFunc();
            }
        }
    }
}
