package net.mimic.monstermod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEvents {
    //エンティティにCapabilityを追加
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).isPresent()) {
                event.addCapability(
                        new ResourceLocation(MonsterMod.MOD_ID, "transformation"),
                        new PlayerTransformationProvider()
                );
            }
        }
    }

    //リスポーンとディメンション移動にデータをコピー
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() || (event.getEntity().level().isClientSide() && !event.getOriginal().level().isClientSide())) {
            event.getOriginal().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(oldCap -> {
                event.getEntity().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(newCap -> {
                    newCap.setTransformed(oldCap.isTransformed());
                    newCap.setTransformedMobId(oldCap.getTransformedMobId());

                    if (oldCap.getTransformedMobId() != null) {
                        PlayerTransformation.MonsterState oldState = oldCap.getMonsterState(oldCap.getTransformedMobId());
                        newCap.setMonsterState(oldCap.getTransformedMobId(), oldState);
                    }
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ResourceLocation mobId = transformation.getTransformedMobId();
                PlayerTransformation.MonsterState state = mobId != null
                        ? transformation.getMonsterState(mobId)
                        : new PlayerTransformation.MonsterState();

                ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                        player.getUUID(),
                        mobId,
                        state.animationState,
                        state.animationTick,
                        state.customFlags
                ), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ResourceLocation mobId = transformation.getTransformedMobId();
                PlayerTransformation.MonsterState state = mobId != null
                        ? transformation.getMonsterState(mobId)
                        : new PlayerTransformation.MonsterState();

                ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                        player.getUUID(),
                        mobId,
                        state.animationState,
                        state.animationTick,
                        state.customFlags
                ), player);
            });
        }
    }
}