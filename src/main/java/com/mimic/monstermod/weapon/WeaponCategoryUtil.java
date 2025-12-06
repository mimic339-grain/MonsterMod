package com.mimic.monstermod.weapon;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WeaponCategoryUtil
 * - Item を WeaponCategory にマッピングする非常に単純なユーティリティ。
 * - 起動時に明示的に registerCategoryItem() でアイテムを登録して使います。
 *
 * 使い方：
 *   WeaponCategoryUtil.registerCategoryItem(WeaponCategory.HAMMER, ModItems.HAMMER_KOKU.get());
 *   // その後
 *   WeaponCategory cat = WeaponCategoryUtil.getCategory(stack);
 */
public final class WeaponCategoryUtil {

    private static final Map<Item, WeaponCategory> ITEM_TO_CATEGORY = new ConcurrentHashMap<>();

    private WeaponCategoryUtil() {}

    /**
     * サーバ起動時（または Mod 初期化時の登録フェーズ）に呼ぶこと。
     * ModItems 等で登録したアイテムをここでカテゴリに紐付ける。
     */
    public static void registerCategoryItem(WeaponCategory category, Item item) {
        if (item == null) return;
        ITEM_TO_CATEGORY.put(item, category == null ? WeaponCategory.NONE : category);
    }

    /**
     * ItemStack からカテゴリを取得。
     * 登録がなければ NONE を返す。
     */
    public static WeaponCategory getCategory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponCategory.NONE;
        return getCategory(stack.getItem());
    }

    public static WeaponCategory getCategory(Item item) {
        if (item == null) return WeaponCategory.NONE;
        return ITEM_TO_CATEGORY.getOrDefault(item, WeaponCategory.NONE);
    }

    /**
     * 開発用：登録済みマッピングをクリア（テスト用）
     */
    public static void clearMappings() {
        ITEM_TO_CATEGORY.clear();
    }
}
