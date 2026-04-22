package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetWeaponSlotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void monstermod$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (isOverWeaponSlot(mouseX, mouseY)) {
            if (button == 0) {
                ModMessages.sendToServer(new C2S_SetWeaponSlotPacket());
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void monstermod$onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (isOverWeaponSlot(mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    private boolean isOverWeaponSlot(double mouseX, double mouseY) {
        if (!((Object)this instanceof InventoryScreen inv)) return false;
        var player = Minecraft.getInstance().player;
        if (player == null || !HunterTransformation.isHunter(player)) return false;

        // レシピ本が開いているかチェック
        boolean bookVisible = inv.getRecipeBookComponent().isVisible();
        // 本がある時は右側(176+α)、ない時は左側(-28)
        int currentXOffset = bookVisible ? this.imageWidth + 4 : -28;

        int slotX = this.leftPos + currentXOffset - 1;
        int slotY = this.topPos + 8 - 1;

        return mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;
    }
}