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

    //@Shadow protected int rightHeight; これは食料ゲージやアーマーゲージのところにゲージを作成しておく場合酸素ゲージと被る可能性があるため必要　只今はそれがないため不必要
    @Shadow protected int leftHeight;

    @Inject(method = "renderHealth", at = @At("HEAD"), cancellable = true)
    private void cancelHealth(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(MonsterTransformation::isTransformed).orElse(false)) {
            ci.cancel();
            this.leftHeight += 10;
        }
    }

    @Inject(method = "pre", at = @At("HEAD"), cancellable = true)
    private void onPreRenderOverlay(NamedGuiOverlay overlay, GuiGraphics guiGraphics, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String path = overlay.id().getPath();

        // 状態の取得
        boolean isTransformed = mc.player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .map(MonsterTransformation::isTransformed).orElse(false);

        IPlayerData playerData = mc.player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).orElse(null);
        if (playerData == null) return;

        // 1. 食料ゲージの判定
        if (path.equals("food_level")) {
            // コマンドで非表示設定になっているか
            if (playerData.hasState(IPlayerData.STATE_HIDE_FOOD)) {
                //this.rightHeight += 10;
                cir.setReturnValue(true);
            }
        }

        // 2. アーマーバーの判定
        if (path.equals("armor_level")) {
            // 「Monsterに変身している」または「コマンドで非表示設定」なら消す
            if (isTransformed || playerData.hasState(IPlayerData.STATE_HIDE_ARMOR)) {
                //this.rightHeight += 10;
                cir.setReturnValue(true);
            }
        }
    }
}