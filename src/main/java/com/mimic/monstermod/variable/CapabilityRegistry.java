package com.mimic.monstermod.variable;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.*;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMonsterCapSyncPacket;
import com.mimic.monstermod.network.server.S2CPlayerCapSyncPacket;
import com.mimic.monstermod.variable.entity.IMonsterData;
import com.mimic.monstermod.variable.entity.IPlayerData;
import com.mimic.monstermod.variable.entity.MonsterData;
import com.mimic.monstermod.variable.entity.PlayerCap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    public static final Capability<IPlayerData> PLAYER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IMonsterData> MONSTER_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<MonsterTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<HunterTransformation> HUNTER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<>() {});

    // ★ 新規追加：ハンター戦闘状態
    public static final Capability<HunterCombatState> HUNTER_COMBAT_STATE =
            CapabilityManager.get(new CapabilityToken<>() {});


    // ==========================================================
    // GETTERS
    // ==========================================================

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

    public static LazyOptional<MonsterTransformation> getPlayerTransformation(Player player) {
        return player.getCapability(PLAYER_TRANSFORMATION);
    }

    public static LazyOptional<HunterTransformation> getHunterTransformation(Player player) {
        return player.getCapability(HUNTER_TRANSFORMATION);
    }

    // ★ 新規追加 Getter
    public static LazyOptional<HunterCombatState> getHunterCombatState(Player player) {
        return player.getCapability(HUNTER_COMBAT_STATE);
    }


    // ==========================================================
    // REGISTER CAPABILITIES
    // ==========================================================
    @SubscribeEvent
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IPlayerData.class);
        event.register(IMonsterData.class);
        event.register(MonsterTransformation.class);
        event.register(HunterTransformation.class);
        event.register(HunterCombatState.class); // ★ 戦闘状態登録
    }


    // ==========================================================
    // ATTACH CAPABILITIES
    // ==========================================================
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {

        Entity entity = event.getObject();

        if (entity instanceof Player player) {

            event.addCapability(PlayerCapabilityProvider.ID, new PlayerCapabilityProvider(player));

            event.addCapability(
                    new ResourceLocation(MonsterMod.MOD_ID, "player_transformation"),
                    new MonsterTransformationProvider()
            );

            event.addCapability(
                    new ResourceLocation(MonsterMod.MOD_ID, "hunter_transformation"),
                    new HunterTransformationProvider()
            );

            // ★ 新規追加：CombatState
            event.addCapability(
                    new ResourceLocation(MonsterMod.MOD_ID, "hunter_combat_state"),
                    new HunterCombatStateProvider()
            );

        } else if (entity instanceof LivingEntity living) {
            event.addCapability(MonsterCapabilityProvider.ID, new MonsterCapabilityProvider(living));
        }
    }


    // ==========================================================
    // COPY CAPABILITIES（リスポ / 次元移動）
    // ==========================================================
    public static void copyCaps(Player oldPlayer, Player newPlayer) {

        oldPlayer.getCapability(PLAYER_CAPABILITY).ifPresent(oldCap ->
                newPlayer.getCapability(PLAYER_CAPABILITY).ifPresent(newCap ->
                        newCap.deserializeNBT(oldCap.serializeNBT())
                )
        );

        oldPlayer.getCapability(MONSTER_CAPABILITY).ifPresent(oldCap ->
                newPlayer.getCapability(MONSTER_CAPABILITY).ifPresent(newCap ->
                        newCap.deserializeNBT(oldCap.serializeNBT())
                )
        );

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

        // ★ 新規追加：CombatState コピー
        oldPlayer.getCapability(HUNTER_COMBAT_STATE).ifPresent(oldCap ->
                newPlayer.getCapability(HUNTER_COMBAT_STATE).ifPresent(newCap ->
                        newCap.deserializeNBT(oldCap.serializeNBT())
                )
        );
    }


    // ==========================================================
    // SYNC（サーバー → クライアント）
    // ==========================================================
    public static void syncToClient(Player player) {

        if (!(player instanceof ServerPlayer sp)) return;

        // -------- base caps --------
        ModMessages.sendToPlayer(
                new S2CPlayerCapSyncPacket(getPlayerData(player).serializeNBT()),
                sp
        );

        ModMessages.sendToPlayer(
                new S2CMonsterCapSyncPacket(getMonsterData(player).serializeNBT()),
                sp
        );

        // -------- transformation --------
        getPlayerTransformation(player).ifPresent(trans -> trans.syncToClient(sp));
        getHunterTransformation(player).ifPresent(trans -> trans.syncToClient(sp));

        // -------- combat state --------
        getHunterCombatState(player).ifPresent(combat -> combat.syncToClient(sp));
    }
}
