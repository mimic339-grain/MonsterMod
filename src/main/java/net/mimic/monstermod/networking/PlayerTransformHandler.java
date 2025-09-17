package net.mimic.monstermod.networking;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.BaseMonsterEntity;
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

            // 変身解除
            if (!transform) {
                int entityId = cap.getTransformedEntityId();
                if (entityId != -1) {
                    Entity e = player.level().getEntity(entityId);
                    if (e != null) {
                        e.discard(); // removeより安全
                    }
                    PlayerTransformTickHandler.removeTransformed(player);
                }

                cap.setTransformed(false);
                cap.setTransformedMobId(null);
                cap.setTransformedEntityId(-1);
                cap.syncToClient(player);

                MonsterMod.getLogger().debug("{} の変身を解除", player.getName().getString());
                return;
            }

            // 変身開始
            if (identityId == null) return;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(identityId);
            if (type == null) return;

            Level level = player.level();
            if (level == null) return;

            Entity entity = type.create(level);
            if (!(entity instanceof BaseMonsterEntity<?> monster)) return;

            // プレイヤーの位置にスポーン
            monster.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            level.addFreshEntity(monster);

            // Capability に情報を保存（Entity インスタンスは保持しない）
            cap.setTransformed(true);
            cap.setTransformedMobId(identityId);
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
