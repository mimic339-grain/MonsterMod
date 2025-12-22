package com.mimic.monstermod.util;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

import java.util.UUID;

public final class HunterUtil {

    private HunterUtil() {}

    private static final UUID MOVE_PENALTY_UUID =
            UUID.fromString("e6c1c3c4-8b9a-4f11-a0b3-1eaa5d0e7a11");

    // ================================================================
    // Capability 取得
    // ================================================================
    public static HunterTransformation getHunter(Player player) {
        LazyOptional<HunterTransformation> cap =
                CapabilityRegistry.getHunterTransformation(player);
        return cap.orElse(null);
    }

    // ================================================================
    // Hotbar 表示（Mixin 側が参照）
    // ================================================================
    public static void enableHotbarRender(Player player) {
        // Mixin / Client側フラグで制御する想定
    }

    public static void disableHotbarRender(Player player) {
        // Mixin / Client側フラグで制御する想定
    }

    // ================================================================
    // 移動速度補正（安全）
    // ================================================================
    public static void applyMovePenalty(Player player, float penalty) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        if (attr.getModifier(MOVE_PENALTY_UUID) != null) return;

        AttributeModifier mod = new AttributeModifier(
                MOVE_PENALTY_UUID,
                "Hunter move penalty",
                -penalty,
                AttributeModifier.Operation.ADDITION
        );
        attr.addPermanentModifier(mod);
    }

    public static void removeMovePenalty(Player player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(MOVE_PENALTY_UUID);
    }
}
