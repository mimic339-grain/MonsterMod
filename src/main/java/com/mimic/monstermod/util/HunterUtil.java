package com.mimic.monstermod.util;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry; // ← 追加
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

public final class HunterUtil {

    private HunterUtil() {}

    // ================================================================
    // HunterTransformation を取得
    // ================================================================
    public static HunterTransformation getHunter(Player player) {
        LazyOptional<HunterTransformation> cap = CapabilityRegistry.getHunterTransformation(player);
        return cap.orElse(null);
    }

    // ================================================================
    // Layer表示（描画処理はRenderer側に委託）
    // ================================================================
    public static void applyLayerWeapon(Player player, ItemStack weapon) {}
    public static void removeHandWeaponLayer(Player player) {}
    public static void enableHotbarRender(Player player) {}
    public static void disableHotbarRender(Player player) {}

    // ================================================================
    // 移動速度補正
    // ================================================================
    public static void applyMovePenalty(Player player, float penalty) {
        var att = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (att == null) return;
        att.setBaseValue(att.getBaseValue() - penalty);
    }

    public static void removeMovePenalty(Player player, float penalty) {
        var att = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (att == null) return;
        att.setBaseValue(att.getBaseValue() + penalty);
    }
}
