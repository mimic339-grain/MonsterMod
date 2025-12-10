package com.mimic.monstermod.gui.hunter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * WeaponSlot
 * -------------
 * GUI上の1スロットを表現。
 * 描画・サーバー同期更新を担当。
 * クリック操作は WeaponSlotWidget が担当。
 */
public class WeaponSlot {

    /** スロット内のアイテム */
    private ItemStack stack = ItemStack.EMPTY;

    /** スロットの位置 */
    private int x, y;

    /** スロットのサイズ（正方形） */
    private final int size;

    public WeaponSlot(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    /** GUI描画処理 */
    public void render(Minecraft mc, GuiGraphics gui) {
        if (stack == null || stack.isEmpty()) return;

        gui.renderItem(stack, x, y);
        gui.renderItemDecorations(mc.font, stack, x, y);
    }

    /** スロット位置更新 */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** スロット内のアイテム取得 */
    public ItemStack getStack() {
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /**
     * サーバーから送られてきた状態で更新
     * マルチプレイヤーでクライアント側の表示を同期
     */
    public void updateFromServer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.stack = ItemStack.EMPTY;
        } else {
            this.stack = stack.copy();
        }
    }
}
