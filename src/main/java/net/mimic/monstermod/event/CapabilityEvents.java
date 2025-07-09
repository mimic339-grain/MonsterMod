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
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).isPresent()) {
                event.addCapability(new ResourceLocation(MonsterMod.MOD_ID, "transformation"), new PlayerTransformationProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() || (event.getEntity().level().isClientSide() && !event.getOriginal().level().isClientSide())) {
            event.getOriginal().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(oldCap -> {
                event.getEntity().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(newCap -> {
                    newCap.setTransformed(oldCap.isTransformed());
                    newCap.setTransformedMobId(oldCap.getTransformedMobId());
                    newCap.setMimicOpen(oldCap.isMimicOpen());
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.isMimicOpen()), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.isMimicOpen()), player);
            });
        }
    }
}