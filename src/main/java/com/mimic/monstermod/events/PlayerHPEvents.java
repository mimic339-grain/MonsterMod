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
    // ダメージ
    // ================================
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double damage = event.getAmount();

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getIdentity() != null) {
                        MonsterTransformUtil.damageIdentity(
                                player,
                                transformation.getIdentity().getId(),
                                damage
                        );
                    } else {
                        MonsterTransformUtil.damagePlayer(player, damage);
                    }
                });
    }

    // ================================
    // 回復
    // ================================
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double heal = event.getAmount();

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getIdentity() != null) {
                        String id = transformation.getIdentity().getId();
                        MonsterTransformUtil.setIdentityHP(
                                player,
                                id,
                                MonsterTransformUtil.getIdentityHP(player, id) + heal
                        );
                    } else {
                        MonsterTransformUtil.setPlayerHP(
                                player,
                                MonsterTransformUtil.getPlayerHP(player) + heal
                        );
                    }
                });
    }

    // ================================
    // PlayerClone（死亡・リスポーン）
    // ================================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        oldPlayer.revive();
        CapabilityRegistry.copyCaps(oldPlayer, newPlayer);

        /* =========================
         * HP / Identity HP
         * ========================= */
        MonsterTransformUtil.setPlayerHP(
                newPlayer,
                MonsterTransformUtil.getPlayerHP(oldPlayer)
        );

        oldPlayer.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(oldTrans -> {
                    if (oldTrans.isTransformed() && oldTrans.getIdentity() != null) {
                        String id = oldTrans.getIdentity().getId();
                        MonsterTransformUtil.setIdentityHP(
                                newPlayer,
                                id,
                                MonsterTransformUtil.getIdentityHP(oldPlayer, id)
                        );
                    }
                });

        /* =========================
         * ★ Hunter Clone 再構築（slot優先設計）
         * ========================= */
        oldPlayer.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .ifPresent(oldHunter ->
                        newPlayer.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                                .ifPresent(newHunter -> {

                                    // weaponSlot は既にコピー済み
                                    if (newHunter.getWeaponSlot().isEmpty()) return;

                                    // 死亡前に Hunter が有効だった場合のみ復元
                                    if (oldHunter.isActive()) {
                                        newHunter.syncEquippedFromSlot(newPlayer);
                                    }
                                })
                );

        /* =========================
         * NBT 同期
         * ========================= */
        MonsterTransformUtil.loadAllFromNBT(oldPlayer);
        MonsterTransformUtil.saveAllToNBT(newPlayer);

        if (newPlayer instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }

    // ================================
    // ログイン
    // ================================
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }

    // ================================
    // リスポーン
    // ================================
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        MonsterTransformUtil.resetPlayerHP(sp);
        MonsterTransformUtil.resetIdentityHPOnRespawn(sp);
        CapabilityRegistry.syncToClient(sp);
    }

    // ================================
    // ディメンション移動
    // ================================
    @SubscribeEvent
    public static void onPlayerDimChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }
}
