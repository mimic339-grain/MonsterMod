package com.mimic.monstermod.gui.hunter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * HunterSlot
 * ----------------------------------------------------
 * このクラスは、自作のアイテムスロット（1枠）の実体を管理するクラス。
 * ▼このクラスが担当すること
 *   - スロット内部のアイテム(ItemStack)
 *   - 画面上の位置 (x, y)
 *   - スロットのサイズ（クリック範囲）
 *   - アイテムの描画（render）
 *   - アイテム入れ替えの処理（handleClick）

 * ▼担当しないこと
 *   - クリックイベントのキャッチ
 *     → クリック判定は HunterSlotWidget（AbstractWidget）が担当。
 *       HunterSlot は Widget に呼ばれるだけ。
 */
public class HunterSlot {

    /** スロットに入っているアイテム */
    private ItemStack stack = ItemStack.EMPTY;

    /** スロットの画面上の位置 */
    private int x;
    private int y;

    /** スロットのサイズ（幅・高さ：正方形） */
    private final int size;

    /**
     * スロットの生成
     *
     * @param x    - スロット描画位置X
     * @param y    - スロット描画位置Y
     * @param size - スロットの幅・高さ
     */
    public HunterSlot(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    /**
     * スロットに入っているアイテムの描画処理
     * @param mc   - Minecraft インスタンス
     * @param gui  - GuiGraphics（描画用のラッパ）
     * スロット背景などは描かず、
     * 「純粋にアイテムだけを描画」する。
     */
    public void render(Minecraft mc, GuiGraphics gui) {
        if (stack.isEmpty()) return;

        // アイテムを描く
        gui.renderItem(stack, x, y);

        // 個数や耐久バーの表示を描く
        gui.renderItemDecorations(mc.font, stack, x, y);
    }

    /**
     * クリックされた時に呼ばれる
     * → HunterSlotWidget から呼び出される
     * このメソッドは
     * 「スロットとカーソルのアイテムを入れ替える」
     * という非常に重要なロジックを担当する。
     */
    public boolean handleClick(Minecraft mc, double mouseX, double mouseY) {

        // クリック座標がスロット範囲内か判定
        if (mouseX >= x && mouseX <= x + size &&
                mouseY >= y && mouseY <= y + size) {

            // プレイヤーが現在カーソルで持っているアイテム
            ItemStack cursor = mc.player.containerMenu.getCarried();

            // 今スロットに入っているアイテムを一旦コピー
            ItemStack temp = stack.copy();

            // スロットにカーソルのアイテムを入れる
            stack = cursor.copy();

            // カーソルに元のスロットのアイテムを戻す
            mc.player.containerMenu.setCarried(temp);

            return true;
        }
        return false;
    }

    /**
     * スロットの位置を動的に変更
     * → InventoryScreen の位置に合わせて毎フレーム更新するために使う
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** 現在のスロット内のアイテムを取得 */
    public ItemStack getStack() { return stack; }

    /** スロットにアイテムをセット（copy して安全に保持） */
    public void setStack(ItemStack stack) {
        this.stack = stack.copy();
    }
}
