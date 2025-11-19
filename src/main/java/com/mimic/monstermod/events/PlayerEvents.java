package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class PlayerEvents {

    // ================================
    // プレイヤークローン（死亡・リスポーン時）
    // ================================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        // 古い Player を一時的に復活（Capability コピーの安全確保用）
        oldPlayer.revive();

        // Capability コピー
        CapabilityRegistry.copyCaps(oldPlayer, newPlayer);

        // PlayerTransformation があれば同期
        CapabilityRegistry.syncToClient(newPlayer);
    }

    // ================================
    // ログイン時
    // ================================
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CapabilityRegistry.syncToClient(serverPlayer);
        }
    }

    // ================================
    // リスポーン時
    // ================================
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CapabilityRegistry.syncToClient(serverPlayer);
        }
    }

    // ================================
    // ディメンション移動時
    // ================================
    @SubscribeEvent
    public static void onPlayerDimChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CapabilityRegistry.syncToClient(serverPlayer);
        }
    }

    // ================================
    // プレイヤー死亡時
    // ================================
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // PlayerHP / IdentityHP をリセット（HPMap側のみ）
        MonsterTransformUtil.resetPlayerHP(player);
        MonsterTransformUtil.resetIdentityHP(player);

        // NBT にも保存
        CompoundTag tag = player.getPersistentData();
        MonsterTransformUtil.saveHPToNBT(player, tag);

        // クライアント同期
        if (player instanceof ServerPlayer serverPlayer) {
            CapabilityRegistry.syncToClient(serverPlayer);
        }
    }
}
