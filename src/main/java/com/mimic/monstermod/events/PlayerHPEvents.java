package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class PlayerHPEvents {

    // ================================
    // ダメージを受けたとき
    // ================================
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double damage = event.getAmount();

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getIdentity() != null) {
                        String identityId = transformation.getIdentity().getId();
                        MonsterTransformUtil.damageIdentity(player, identityId, damage);
                    } else {
                        MonsterTransformUtil.damagePlayer(player, damage);
                    }
                });
    }

    // ================================
    // 自然回復 / 回復アイテム
    // ================================
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double healAmount = event.getAmount();

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getIdentity() != null) {
                        String identityId = transformation.getIdentity().getId();
                        double newHP = MonsterTransformUtil.getIdentityHP(player, identityId) + healAmount;
                        MonsterTransformUtil.setIdentityHP(player, identityId, newHP);
                    } else {
                        double newHP = MonsterTransformUtil.getPlayerHP(player) + healAmount;
                        MonsterTransformUtil.setPlayerHP(player, newHP);
                    }
                });
    }

    // ================================
    // プレイヤークローン時（死亡・リスポーン）
    // ================================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        // 古い Player を一時的に復活（Capability コピーの安全確保用）
        oldPlayer.revive();

        // Capability コピー
        CapabilityRegistry.copyCaps(oldPlayer, newPlayer);

        // PlayerHP をコピー
        double oldPlayerHP = MonsterTransformUtil.getPlayerHP(oldPlayer);
        MonsterTransformUtil.setPlayerHP(newPlayer, oldPlayerHP);

        // IdentityHP をコピー（変身中の Identity のみ）
        oldPlayer.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getIdentity() != null) {
                        String identityId = transformation.getIdentity().getId();
                        double oldIdentityHP = MonsterTransformUtil.getIdentityHP(oldPlayer, identityId);
                        MonsterTransformUtil.setIdentityHP(newPlayer, identityId, oldIdentityHP);
                    }
                });

        // NBT データもコピー
        MonsterTransformUtil.loadAllFromNBT(oldPlayer);
        MonsterTransformUtil.saveAllToNBT(newPlayer);

        // クライアント同期
        if (newPlayer instanceof ServerPlayer serverPlayer) {
            CapabilityRegistry.syncToClient(serverPlayer);
        }
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
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        // HP が 0 の Identity を最大値に復活
        MonsterTransformUtil.resetIdentityHPOnRespawn(serverPlayer);
        // クライアント同期
        CapabilityRegistry.syncToClient(serverPlayer);
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
}
