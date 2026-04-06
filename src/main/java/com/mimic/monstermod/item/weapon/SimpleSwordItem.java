package com.mimic.monstermod.item.weapon;

import com.mimic.monstermod.geo.renderer.item.SimpleSwordRenderer;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * ただの「片手剣」カテゴリ武器。
 * ・カテゴリ保持のみ
 * ・攻撃処理は HunterCombatState が全部やる
 * ・GeoRenderer は WeaponItem が提供する
 */
public class SimpleSwordItem extends WeaponItem {
    private static final SimpleSwordRenderer RENDERER = new SimpleSwordRenderer();

    public SimpleSwordItem() {
        super(
                new Item.Properties()
                        .stacksTo(1)
                        .durability(250),
                WeaponCategory.SWORD
        );
    }

    @Override
    public GeoItemRenderer<? extends WeaponItem> getRenderer() {
        return RENDERER;
    }
}