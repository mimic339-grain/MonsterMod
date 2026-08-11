package com.mimic.monstermod.entity.hitbox;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Yatagarasuの部位ごとの弱点設定(ダメージ倍率・被弾音)を1箇所にまとめたもの。
 * ボーン名は yatagarasu.geo.json の hitbox_* ボーン名と一致させること。
 */
public final class YatagarasuHitboxProfile {

    private YatagarasuHitboxProfile() {}

    public record PartConfig(String boneName, float damageMultiplier, @Nullable SoundEvent hitSound) {}

    public static final List<PartConfig> PARTS = List.of(
            new PartConfig("hitbox_head", 2.0f, SoundEvents.PLAYER_ATTACK_CRIT),
            new PartConfig("hitbox_body", 1.0f, null),
            new PartConfig("hitbox_tail", 0.75f, null),
            new PartConfig("hitbox_RightWingOver", 1.0f, null),
            new PartConfig("hitbox_RightWingUnder", 1.0f, null),
            new PartConfig("hitbox_LeftWingOver", 1.0f, null),
            new PartConfig("hitbox_LeftWingUnder", 1.0f, null),
            new PartConfig("hitbox_foot", 0.75f, null)
    );
}
