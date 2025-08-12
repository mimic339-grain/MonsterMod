package com.mimic.monster.transform;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mimic.monster.network.NetworkHandler;
import com.mimic.monster.network.client.S2CUpdateTransformPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

public final class TransformHandler {

    private TransformHandler() {}

    /**
     * 指定プレイヤーを targetType に“変身”させる（サーバー側処理）
     * - 元の値を capability に保存する
     * - 属性を変身先のデフォルト値に合わせる
     * - プレイヤーの姿勢・サイズを更新（refreshDimensions）
     * - capability を更新してクライアントへ同期
     */
    public static void transformPlayer(ServerPlayer player, EntityType<? extends LivingEntity> targetType) {
        if (player == null || targetType == null) return;

        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            // すでに変身中なら何もしない
            if (cap.isTransformed()) return;

            // --- 元の値を capability に保存 ---
            // MAX_HEALTH / ATTACK_DAMAGE / 幅・高さ（bb width/height）
            if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                cap.setOriginalMaxHealth(player.getAttribute(Attributes.MAX_HEALTH).getBaseValue());
            } else {
                cap.setOriginalMaxHealth(player.getMaxHealth());
            }

            if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                cap.setOriginalAttackDamage(player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
            } else {
                cap.setOriginalAttackDamage(0.0D);
            }

            cap.setOriginalWidth(player.getBbWidth());
            cap.setOriginalHeight(player.getBbHeight());

            // --- 変身先のデフォルト値を取得（安全に） ---
            LivingEntity dummy = null;
            try {
                var created = targetType.create(player.level());
                if (created instanceof LivingEntity) {
                    dummy = (LivingEntity) created;
                } else {
                    // 変身先が LivingEntity でないならキャンセル
                    return;
                }
            } catch (Exception e) {
                // create が例外を投げる可能性があるので守る
                return;
            }

            // --- 属性を変身先に合わせる（サーバーで直接 baseValue を変える方式） ---
            if (dummy != null) {
                if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                    double newMax = dummy.getMaxHealth();
                    player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
                    player.setHealth((float)Math.min(player.getHealth(), newMax)); // 現在HPが上限を超えないように
                }

                if (dummy.getAttribute(Attributes.ATTACK_DAMAGE) != null && player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                    player.getAttribute(Attributes.ATTACK_DAMAGE)
                            .setBaseValue(dummy.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
                }

                if (dummy.getAttribute(Attributes.MOVEMENT_SPEED) != null && player.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                    player.getAttribute(Attributes.MOVEMENT_SPEED)
                            .setBaseValue(dummy.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue());
                }

                // --- 見た目（姿勢）・サイズの反映 ---
                // 直接 BoundingBox を書き換えるのは避け、Pose を合わせて refreshDimensions() を呼ぶ
                player.setPose(dummy.getPose() != null ? dummy.getPose() : Pose.STANDING);
                player.refreshDimensions();
            }

            // --- capability を更新 ---
            cap.setTransformedType(targetType);
            cap.setTransformed(true);

            // --- クライアントへ同期（変身開始） ---
            // NetworkHandler の実装に合わせて、EntityType -> ResourceLocation などに変換して送る
            var typeId = ForgeRegistries.ENTITY_TYPES.getKey(targetType);

            try {
                NetworkHandler.sendToAllTracking(new S2CUpdateTransformPacket(player.getId(), typeId), player);
            } catch (Exception e) {
                System.err.println("Failed to send transform packet: " + e.getMessage());
            }
        });
    }

    /**
     * 変身解除（サーバー側）
     * - capability に保存していた元の値を復元する
     * - プレイヤーの姿勢とサイズをリフレッシュ
     * - capability をクリアしてクライアントへ同期
     */
    public static void revertTransform(ServerPlayer player) {
        if (player == null) return;

        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            if (!cap.isTransformed()) return;

            // --- 元の属性値を capability から復元 ---
            if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(cap.getOriginalMaxHealth());
                player.setHealth((float)Math.min(player.getHealth(), cap.getOriginalMaxHealth()));
            }

            if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(cap.getOriginalAttackDamage());
            }

            if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                // Movement Speed は capabilityに保存がないならデフォルト値に戻すかも？
                // ここは必要なら Capability に保存を追加して復元する形にする
            }

            // --- 見た目・当たり判定を元に戻す ---
            player.setPose(Pose.STANDING);
            player.refreshDimensions();

            // --- capability をクリア ---
            cap.setTransformed(false);
            cap.setTransformedType(null);

            // --- クライアントへ同期（変身解除） ---
            try {
                NetworkHandler.sendToAllTracking(
                        new S2CUpdateTransformPacket(player.getId(),
                                new net.minecraft.resources.ResourceLocation("monstermod", "player")),
                        player);
            } catch (Exception e) {
                System.err.println("Failed to send revert transform packet: " + e.getMessage());
            }
        });
    }
}