package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PlayerTransformationMixinHelper {

    /**
     * サーバー側でプレイヤー変身情報をクライアントに同期
     */
    public static void syncTransformation(ServerPlayer player) {
        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    // 変身中かどうかで属性設定
                    if (trans.isTransformed()) {
                        LivingEntity identityEntity = trans.getEntity();
                        if (identityEntity != null) {
                            applyEntityAttributesToPlayer(player, identityEntity);
                        }
                    } else {
                        resetPlayerAttributes(player);
                    }

                    // クライアント同期
                    ModMessages.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new S2CTransformSyncPacket(player.getUUID(), trans.serializeNBT())
                    );
                });
    }

    /**
     * プレイヤー属性を Identity Entity に合わせる
     */
    private static void applyEntityAttributesToPlayer(ServerPlayer player, LivingEntity identity) {
        syncAttribute(player, Attributes.MAX_HEALTH, identity);
        syncAttribute(player, Attributes.ATTACK_DAMAGE, identity);
        syncAttribute(player, Attributes.MOVEMENT_SPEED, identity);
        syncAttribute(player, Attributes.ARMOR, identity);
        syncAttribute(player, Attributes.KNOCKBACK_RESISTANCE, identity);

        // BaseMonsterEntity固有のカスタム属性
        if (identity instanceof BaseMonsterEntity bme) {
            syncAttribute(player, BaseMonsterEntity.GRAVITY, bme);
        }

        // ヘルスを上限に収める
        player.setHealth(Math.min(player.getHealth(), (float) identity.getAttributeValue(Attributes.MAX_HEALTH)));
    }

    /**
     * プレイヤー属性を通常のスティーブにリセット
     */
    private static void resetPlayerAttributes(ServerPlayer player) {
        setAttribute(player, Attributes.MAX_HEALTH, 20.0);
        setAttribute(player, Attributes.ATTACK_DAMAGE, 2.0);
        setAttribute(player, Attributes.MOVEMENT_SPEED, 0.1);
        setAttribute(player, Attributes.ARMOR, 0.0);
        setAttribute(player, Attributes.KNOCKBACK_RESISTANCE, 0.0);
        setAttribute(player, BaseMonsterEntity.GRAVITY, 1.0);

        // ヘルスも上限に合わせて
        player.setHealth((float) player.getAttributeValue(Attributes.MAX_HEALTH));
    }

    /**
     * 共通: プレイヤー属性設定
     */
    private static void syncAttribute(ServerPlayer player, Attribute attr, LivingEntity identity) {
        if (player.getAttribute(attr) != null && identity.getAttribute(attr) != null) {
            player.getAttribute(attr).setBaseValue(identity.getAttributeValue(attr));
        }
    }

    private static void setAttribute(ServerPlayer player, Attribute attr, double value) {
        if (player.getAttribute(attr) != null) {
            player.getAttribute(attr).setBaseValue(value);
        }
    }
}
