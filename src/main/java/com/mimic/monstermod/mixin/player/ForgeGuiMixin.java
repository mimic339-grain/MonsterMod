package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.MonsterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = ForgeGui.class, remap = false)
public abstract class ForgeGuiMixin {
    @Shadow
    public int leftHeight;

    /**
     * 変身中はバニラのハートを消し、代わりにモンスター用のHPバーを出している。
     * ただしボマーのように見た目が変わらない役職は、モンスター用バーも出さないので
     * ここで消すと「ハートも無い・バーも無い」状態になってしまう。
     * そのため体を持つ役職のときだけ消す。
     */
    @Inject(method = "renderHealth", at = @At("HEAD"), cancellable = true)
    private void cancelHealth(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        boolean hideHearts = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(trans -> trans.isTransformed()
                        && (trans.getIdentity() == null || trans.getIdentity().hasOwnBody()))
                .orElse(false);

        if (hideHearts) {
            ci.cancel();
            this.leftHeight += 10;
        }
    }

    @Inject(method = "pre", at = @At("HEAD"), cancellable = true)
    private void onPreRenderOverlay(NamedGuiOverlay overlay, GuiGraphics guiGraphics, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String path = overlay.id().getPath();

        // 状態の安全な取得
        boolean isTransformed = mc.player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(MonsterTransformation::isTransformed).orElse(false);

        // ★修正ポイント：.map を使って null チェックを内包する
        boolean isFoodHidden = mc.player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY)
                .map(cap -> cap.hasState(IPlayerData.STATE_HIDE_FOOD)).orElse(false);
        boolean isArmorHidden = mc.player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY)
                .map(cap -> cap.hasState(IPlayerData.STATE_HIDE_ARMOR)).orElse(false);

        // 1. 食料ゲージの判定
        if (path.equals("food_level") && isFoodHidden) {
            cir.setReturnValue(true); // 描画をキャンセル
        }

        // 2. アーマーバーの判定
        if (path.equals("armor_level")) {
            if (isTransformed || isArmorHidden) {
                cir.setReturnValue(true);
            }
        }
    }
}