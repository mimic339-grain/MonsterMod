package net.mimic.monstermod.variable;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.server.S2CPlayerCapSyncPacket;
import net.mimic.monstermod.networking.server.S2CMonsterCapSyncPacket;
import net.mimic.monstermod.variable.entity.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.LazyOptional;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class CapabilityRegistry {

    // ====== CAPABILITIES ======
    public static final Capability<IPlayerData> PLAYER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IMonsterData> MONSTER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ====== GETTERS ======
    public static IPlayerData getPlayerData(LivingEntity entity) {
        return entity.getCapability(PLAYER_CAPABILITY).orElseGet(() -> new PlayerCap(entity));
    }

    public static IMonsterData getMonsterData(LivingEntity entity) {
        return entity.getCapability(MONSTER_CAPABILITY).orElseGet(() -> new MonsterData(entity));
    }

    public static LazyOptional<IPlayerData> getPlayerLazy(LivingEntity entity) {
        return entity.getCapability(PLAYER_CAPABILITY);
    }

    public static LazyOptional<IMonsterData> getMonsterLazy(LivingEntity entity) {
        return entity.getCapability(MONSTER_CAPABILITY);
    }

    // ====== REGISTER ======
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IPlayerData.class);
        event.register(IMonsterData.class);
    }

    // ====== ATTACH PROVIDERS ======
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            event.addCapability(PlayerCapabilityProvider.ID, new PlayerCapabilityProvider(player));
        }
        else if (entity instanceof LivingEntity living) {
            event.addCapability(MonsterCapabilityProvider.ID, new MonsterCapabilityProvider(living));
        }
    }

    // ====== CLONE / RESPAWN / LOGIN HANDLING ======
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        oldPlayer.revive();

        // PLAYER CAP
        oldPlayer.getCapability(PLAYER_CAPABILITY).ifPresent(oldCap ->
                newPlayer.getCapability(PLAYER_CAPABILITY).ifPresent(newCap -> {
                    CompoundTag tag = oldCap.serializeNBT();
                    newCap.deserializeNBT(tag);
                })
        );

        // MONSTER CAP
        oldPlayer.getCapability(MONSTER_CAPABILITY).ifPresent(oldCap ->
                newPlayer.getCapability(MONSTER_CAPABILITY).ifPresent(newCap -> {
                    CompoundTag tag = oldCap.serializeNBT();
                    newCap.deserializeNBT(tag);
                })
        );

        syncToClient(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerDimChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }
    // ====== SYNC ======
    public static void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        IPlayerData playerData = getPlayerData(player);
        IMonsterData monsterData = getMonsterData(player);

        ModMessages.sendToPlayer(new S2CPlayerCapSyncPacket(playerData.serializeNBT()), serverPlayer);
        ModMessages.sendToPlayer(new S2CMonsterCapSyncPacket(monsterData.serializeNBT()), serverPlayer);
    }
}
