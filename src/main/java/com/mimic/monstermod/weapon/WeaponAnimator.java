package com.mimic.monstermod.weapon;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client 側 Weapon animation 管理
 */
public class WeaponAnimator {

    /**
     * Player → 現在表示すべき WeaponStack
     * WeakHashMap = Player GC 安全
     */
    private static final Map<AbstractClientPlayer, ItemStack> WEAPON_CACHE =
            new WeakHashMap<>();

    /* ============================= */

    /**
     * HunterAnimationController / Layer から呼ばれる
     */
    public static void playWeaponAnimation(
            AbstractClientPlayer player,
            String animationId
    ) {
        ItemStack stack = getWeaponStack(player);
        if (stack.isEmpty()) return;

        if (!(stack.getItem() instanceof WeaponItem weapon)) return;

        // ★ 修正点：player を渡す
        weapon.playWeaponAnimation(player, stack, animationId);
    }

    /**
     * Renderer / Layer から呼ばれる
     */
    public static ItemStack getWeaponStack(AbstractClientPlayer player) {
        return WEAPON_CACHE.computeIfAbsent(player, WeaponAnimator::resolveWeapon);
    }

    /**
     * capability から武器を取得
     */
    private static ItemStack resolveWeapon(AbstractClientPlayer player) {
        return player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .map(HunterTransformation::getWeaponSlot)
                .orElse(ItemStack.EMPTY);
    }

    /**
     * WeaponSlot 変更時に必ず呼ぶ
     */
    public static void invalidate(AbstractClientPlayer player) {
        WEAPON_CACHE.remove(player);
    }
}
