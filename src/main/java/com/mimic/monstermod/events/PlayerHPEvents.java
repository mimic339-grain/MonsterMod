package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.command.ModGameRules;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class PlayerHPEvents {

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed() && trans.getIdentity() != null) {
                String id = trans.getIdentity().getId();
                double newHP = MonsterTransformUtil.getIdentityHP(player, id) - event.getAmount();
                MonsterTransformUtil.setIdentityHP(player, id, newHP);
                player.setHealth((float) Math.max(0, newHP));
            } else {
                float newPlayerHP = player.getHealth() - event.getAmount();
                MonsterTransformUtil.setPlayerHP(player, (double)newPlayerHP);
            }

            if (player instanceof ServerPlayer sp) {
                CapabilityRegistry.syncToClient(sp);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transformation.isTransformed() && transformation.getIdentity() != null) {
                String id = transformation.getIdentity().getId();
                double newHP = MonsterTransformUtil.getIdentityHP(player, id) + event.getAmount();
                MonsterTransformUtil.setIdentityHP(player, id, newHP);
                player.setHealth((float) Math.min(player.getMaxHealth(), newHP));
            } else {
                float finalHP = Math.min(player.getMaxHealth(), player.getHealth() + event.getAmount());
                MonsterTransformUtil.setPlayerHP(player, (double)finalHP);
            }

            if (player instanceof ServerPlayer sp) {
                CapabilityRegistry.syncToClient(sp);
            }
        });
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        oldPlayer.reviveCaps();
        CapabilityRegistry.copyCaps(oldPlayer, newPlayer);

        if (event.isWasDeath()) {
            newPlayer.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                boolean keepTransform = newPlayer.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_KEEP_TRANSFORM);

                if (!keepTransform) {
                    trans.stopTransformation(newPlayer);
                }
                // ★ここでは HP をいじらない（まだ属性が Steve なので）
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // ゲームルールの取得
            boolean keepTransform = player.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_KEEP_TRANSFORM);
            boolean resetHp = player.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_RESET_HP);

            // 1. まず「復活後の変身状態」を確定させる
            if (!keepTransform) {
                trans.stopTransformation(player); // Playerに戻す
            }

            // 2. HPリセット処理（Map内のHP0問題を解決する）
            if (resetHp) {
                // パターン3 & 4: 両方全回復
                MonsterTransformUtil.resetPlayerHP(player);
                MonsterTransformUtil.resetIdentityHP(player);
            } else {
                if (keepTransform) {
                    // パターン1: 変身維持だが、死んだ時のIdentityだけは復活させないと詰む
                    if (trans.isTransformed() && trans.getMobId() != null) {
                        String currentId = trans.getMobId().toString();
                        double max = MonsterTransformUtil.getIdentityMaxHP(player);
                        MonsterTransformUtil.setIdentityHP(player, currentId, max);
                    }
                } else {
                    // パターン2: 変身解除。PlayerHPだけ全回復させる
                    MonsterTransformUtil.resetPlayerHP(player);
                }
            }

            // 3. 属性の適用（MaxHPを20か100かに確定させる）
            MonsterTransformUtil.applyFullTransformation(player, trans);

            // 4. 最終的なHPセット（HP 0 防護策）
            double finalHP;
            if (trans.isTransformed()) {
                String id = trans.getMobId().toString();
                finalHP = MonsterTransformUtil.getIdentityHP(player, id);
            } else {
                finalHP = MonsterTransformUtil.getPlayerHP(player);
            }

            // もし何らかの理由でHPが0以下なら、最低でも1（あるいは最大値）にする
            if (finalHP <= 0) {
                finalHP = player.getMaxHealth();
            }
            player.setHealth((float) finalHP);

            // 5. 同期
            if (player instanceof ServerPlayer sp) {
                CapabilityRegistry.syncToClient(sp);
                MonsterTransformUtil.saveAllToNBT(player);
            }
        });
    }
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerDimChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CapabilityRegistry.syncToClient(sp);
        }
    }
}