package com.mimic.monstermod.variable;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.*;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CPlayerCapSyncPacket;
import com.mimic.monstermod.variable.entity.IPlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Capability を一括管理する巨大レジストリ
 * HunterCombatState まで追加した完全版
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class CapabilityRegistry {

    // ==========================================================
    // CAPABILITIES
    // ==========================================================
    public static final Capability<MonsterTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<HunterTransformation> HUNTER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IPlayerData> PLAYER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ==========================================================
    // GETTERS
    // ==========================================================
    public static LazyOptional<MonsterTransformation> getPlayerTransformation(Player player) {
        return player.getCapability(PLAYER_TRANSFORMATION);
    }

    public static LazyOptional<HunterTransformation> getHunterTransformation(Player player) {
        return player.getCapability(HUNTER_TRANSFORMATION);
    }

    public static LazyOptional<IPlayerData> getPlayerData(Player player) {
        return player.getCapability(PLAYER_CAPABILITY);
    }

    // ==========================================================
    // REGISTER CAPABILITIES
    // ==========================================================
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(MonsterTransformation.class);
        event.register(HunterTransformation.class);
        event.register(IPlayerData.class);
    }


    // ==========================================================
    // ATTACH CAPABILITIES
    // ==========================================================
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {

        Entity entity = event.getObject();

        if (entity instanceof Player player) {

            event.addCapability(
                    new ResourceLocation(MonsterMod.MOD_ID, "player_transformation"),
                    new MonsterTransformationProvider()
            );

            event.addCapability(
                    new ResourceLocation(MonsterMod.MOD_ID, "hunter_transformation"),
                    new HunterTransformationProvider()
            );

            event.addCapability(PlayerCapabilityProvider.ID, new PlayerCapabilityProvider(player));
        }
    }


    // ==========================================================
    // COPY CAPABILITIES（リスポ / 次元移動）
    // ==========================================================
    public static void copyCaps(Player oldPlayer, Player newPlayer) {

        oldPlayer.getCapability(PLAYER_TRANSFORMATION).ifPresent(oldCap ->
                newPlayer.getCapability(PLAYER_TRANSFORMATION).ifPresent(newCap -> {
                    // 全てのフラグ（isTransformed, keepTransform, resetHp）をコピー
                    CompoundTag tag = oldCap.serializeNBT();
                    newCap.deserializeNBT(tag);

                })
        );
        oldPlayer.getCapability(HUNTER_TRANSFORMATION).ifPresent(oldCap ->
                newPlayer.getCapability(HUNTER_TRANSFORMATION).ifPresent(newCap -> {
                    newCap.deserializeNBT(oldCap.serializeNBT());
                    newCap.onLoad(newPlayer);
                })
        );

        oldPlayer.getCapability(PLAYER_CAPABILITY).ifPresent(oldCap ->
                newPlayer.getCapability(PLAYER_CAPABILITY).ifPresent(newCap ->
                        newCap.deserializeNBT(oldCap.serializeNBT())
                )
        );
    }


    // ==========================================================
    // SYNC（サーバー → クライアント）
    // ==========================================================
    public static void syncToClient(Player player) {

        if (!(player instanceof ServerPlayer sp)) return;
        getPlayerData(player).ifPresent(cap -> {
            ModMessages.sendToPlayer(
                    new S2CPlayerCapSyncPacket(cap.serializeNBT()),
                    sp
            );
        });

        // -------- transformation --------
        getPlayerTransformation(player).ifPresent(trans -> trans.syncToClient(sp));
        getHunterTransformation(player).ifPresent(trans -> trans.syncToClient(sp));
    }
}
