package com.mimic.monstermod.util;

import com.mimic.monstermod.capability.HunterCombatState;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class CombatUtil {

    private CombatUtil() {}

    public static void performMeleeAttack(ServerPlayer player, ItemStack weaponStack,
                                          HunterTransformation ht, HunterCombatState cs) {
        if (player == null) return;
        Level world = player.getCommandSenderWorld();
        if (world == null || world.isClientSide()) return;
        if (cs.isStiff()) return;

        cs.updateCategory(weaponStack);
        WeaponCategory cat = cs.getCurrentCategory();

        int comboStage = cs.getComboStep();
        float damage = calculateMeleeDamage(player, weaponStack, cat, cs, comboStage);

        List<LivingEntity> targets = getTargetsInFront(player, cat.getAttackRange(), 80.0f,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            applyDamageToEntity(player, target, damage);
        }

        cs.advanceCombo();
        int stiffness = cat.getStiffnessForStage(Math.min(comboStage, cat.getComboMax() - 1));
        cs.applyStiffness(stiffness);
    }

    public static float calculateMeleeDamage(Player player, ItemStack weaponStack,
                                             WeaponCategory cat, HunterCombatState cs, int comboStage) {
        float base = cat.getDamageForStage(Math.min(comboStage, cat.getComboMax() - 1));
        base = Math.max(base, cs.getAttackDamage());
        return base;
    }

    public static void applyDamageToEntity(ServerPlayer attacker, LivingEntity target, float damage) {
        if (attacker == null || target == null) return;
        // 重要：world チェック（サーバー側のみ）
        Level world = attacker.serverLevel();  // 1.20.x ではこれが正解
        if (world == null) return;
        // --- ダメージソースの取得 ---
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        // --- ダメージ適用 ---
        target.hurt(source, damage);
    }


    public static List<LivingEntity> getTargetsInFront(Player player, double range, float coneDeg,
                                                       Predicate<LivingEntity> filter) {
        if (player == null) return List.of();
        Level world = player.getCommandSenderWorld();
        if (world == null) return List.of();

        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        AABB box = new AABB(
                player.getX() - range, player.getY() - 2.0, player.getZ() - range,
                player.getX() + range, player.getY() + 2.0, player.getZ() + range
        );

        List<LivingEntity> results = new ArrayList<>();
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && (filter == null || filter.test(e)));

        double coneCos = Math.cos(Math.toRadians(coneDeg / 2.0));
        for (LivingEntity e : candidates) {
            Vec3 dir = e.getEyePosition(1.0f).subtract(eye).normalize();
            double dot = dir.dot(look);
            if (dot >= coneCos) {
                double distSq = e.distanceToSqr(player);
                if (distSq <= range * range + 1e-6) results.add(e);
            }
        }
        return results;
    }

    public static void applyMovePenalty(Player player, float multiplier) {
        if (player == null) return;
        try {
            var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(0.1D * multiplier);
        } catch (Throwable ignored) {}
    }

    public static void removeMovePenalty(Player player) {
        if (player == null) return;
        try {
            var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(0.1D);
        } catch (Throwable ignored) {}
    }

    public static void handleAttackInput(ServerPlayer player, ItemStack weaponStack,
                                         HunterTransformation ht, HunterCombatState cs) {
        if (player == null) return;
        Level world = player.getCommandSenderWorld();
        if (world == null || world.isClientSide()) return;
        if (ht == null || !ht.isActive()) return;
        if (!ht.isSheathed()) { /* 抜刀状態 */ }
        if (cs.isStiff()) return;

        performMeleeAttack(player, weaponStack, ht, cs);
    }


}
