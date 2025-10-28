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
                    // サーバー側属性更新
                    if (trans.isTransformed()) {
                        LivingEntity identityEntity = trans.getTransformedEntity(player.level());
                        if (identityEntity != null) syncPlayerAttributes(player, identityEntity);
                    }

                    // クライアント同期
                    ModMessages.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new S2CTransformSyncPacket(player.getUUID(), trans.serializeNBT())
                    );
                });
    }

    /**
     * プレイヤー属性を Identity に合わせる
     */
    private static void syncPlayerAttributes(ServerPlayer player, LivingEntity identity) {
        if (player == null || identity == null) return;

        syncAttribute(player, Attributes.MAX_HEALTH, identity);
        player.setHealth(Math.min(player.getHealth(), (float) identity.getAttributeValue(Attributes.MAX_HEALTH)));

        syncAttribute(player, Attributes.ATTACK_DAMAGE, identity);
        syncAttribute(player, Attributes.MOVEMENT_SPEED, identity);
        syncAttribute(player, Attributes.ARMOR, identity);
        syncAttribute(player, Attributes.KNOCKBACK_RESISTANCE, identity);
        syncAttribute(player, BaseMonsterEntity.GRAVITY, identity);
    }

    private static void syncAttribute(ServerPlayer player, Attribute attr, LivingEntity identity) {
        if (player.getAttribute(attr) != null && identity.getAttribute(attr) != null) {
            player.getAttribute(attr).setBaseValue(identity.getAttributeValue(attr));
        }
    }
}