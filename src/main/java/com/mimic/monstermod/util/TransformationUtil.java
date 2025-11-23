package com.mimic.monstermod.util;

import com.mimic.monstermod.capability.PlayerTransformation;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TransformationUtil 完全版（無限再帰防止済み）
 */
public class TransformationUtil {

    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> false);

    @Nullable
    public static Entity getEffectiveEntity(@Nullable Entity entity) {
        if (entity == null) return null;
        if (REENTRY.get()) return null;
        REENTRY.set(true);
        try {
            if (!(entity instanceof Player player)) return null;

            LazyOptional<PlayerTransformationProvider> opt = CapabilityRegistry.getPlayerTransformation(player);
            if (opt.isPresent()) {
                PlayerTransformationProvider provider = opt.orElse(null);
                if (provider != null) {
                    PlayerTransformation trans = provider.get();
                    if (trans != null && trans.isTransformed()) {
                        Entity e = trans.getEntity();
                        if (e != null && e != entity) {
                            return e;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            REENTRY.set(false);
        }
        return null;
    }

    public static boolean isTransformed(@Nullable Entity entity) {
        return getEffectiveEntity(entity) != null;
    }

    @Nullable
    public static PlayerTransformation getPlayerTransformation(@Nullable Player player) {
        if (player == null) return null;
        try {
            LazyOptional<PlayerTransformationProvider> opt = CapabilityRegistry.getPlayerTransformation(player);
            if (opt.isPresent()) {
                PlayerTransformationProvider provider = opt.orElse(null);
                if (provider != null) return provider.get();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @NotNull
    public static AABB getBoundingBox(@NotNull Entity entity) {
        if (REENTRY.get()) return entity.getBoundingBox();
        REENTRY.set(true);
        try {
            Entity effective = getEffectiveEntity(entity);
            if (effective != null && effective != entity) {
                // ここでは絶対に再帰呼び出しをしない
                return effective.getBoundingBox();
            }
            return entity.getBoundingBox();
        } finally {
            REENTRY.set(false);
        }
    }

    @NotNull
    public static EntityDimensions getDimensions(@NotNull Player player, Pose pose) {
        if (REENTRY.get()) return player.getDimensions(pose);
        REENTRY.set(true);
        try {
            Entity effective = getEffectiveEntity(player);
            if (effective != null && effective != player) return effective.getDimensions(pose);
        } catch (Exception ignored) {
        } finally {
            REENTRY.set(false);
        }
        return player.getDimensions(pose);
    }

    public static float getEyeHeight(@NotNull Player player, Pose pose) {
        PlayerTransformation trans = getPlayerTransformation(player);
        if (trans != null && trans.isTransformed()) {
            Entity effective = trans.getEntity();
            if (effective instanceof BaseMonsterEntity bme) return bme.getEyeHeight(pose);
            if (effective != null) return effective.getEyeHeight(pose); // Player 以外なら委譲
        }
        return player.getEyeHeight(pose);
    }

    public static void updateFluidHeightAndDoFluidPushing(@NotNull Entity entity) {
        if (REENTRY.get()) {
            entity.updateFluidHeightAndDoFluidPushing();
            return;
        }
        REENTRY.set(true);
        try {
            Entity effective = getEffectiveEntity(entity);
            if (effective != null && effective != entity) effective.updateFluidHeightAndDoFluidPushing();
            else entity.updateFluidHeightAndDoFluidPushing();
        } finally {
            REENTRY.set(false);
        }
    }

    public static boolean isOnFire(@NotNull Entity entity) {
        try {
            Entity effective = getEffectiveEntity(entity);
            if (effective != null && effective != entity) return effective.isOnFire();
            return entity.isOnFire();
        } catch (Exception ignored) {
            return entity.isOnFire();
        }
    }
}
