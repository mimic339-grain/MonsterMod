package net.mimic.monstermod.networking;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;

public class PlayerTransformHandler {

    public static void handleTransformRequest(ServerPlayer player, boolean transform, net.minecraft.resources.ResourceLocation identityId) {
        if (player == null) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {

            if (!transform) {
                BaseMonsterEntity<?> entity = cap.getTransformedEntity();
                if (entity != null) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    PlayerTransformTickHandler.removeTransformed(player);
                }

                cap.setTransformed(false);
                cap.setTransformedMobId(null);
                cap.setTransformedEntity(null);
                cap.setTransformedEntityId(-1);
                cap.syncToClient(player);

                MonsterMod.getLogger().debug("{} の変身を解除", player.getName().getString());
                return;
            }

            if (identityId == null) return;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(identityId);
            if (type == null) return;

            Level level = player.level();
            if (level == null) return;

            Entity entity = type.create(level);
            if (!(entity instanceof BaseMonsterEntity<?> monster)) return;

            monster.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            level.addFreshEntity(monster);

            cap.setTransformed(true);
            cap.setTransformedMobId(identityId);
            cap.setTransformedEntity(monster);
            cap.setTransformedEntityId(monster.getId());

            PlayerTransformTickHandler.registerTransformed(player, monster);

            cap.syncToClient(player);
            MonsterMod.getLogger().debug("{} を {} に変身させました", player.getName().getString(), identityId);
        });
    }

    public static PlayerTransformation getOrCreateTransformation(LocalPlayer player) {
        if (player == null) return null;
        LazyOptional<PlayerTransformation> cap = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION);
        return cap.orElse(null);
    }
}
