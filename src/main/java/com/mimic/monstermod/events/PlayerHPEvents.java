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
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MonsterTransformUtil.saveAllToNBT(sp);
            MonsterMod.LOGGER.info("[LogoutSave] {} のデータを保存しました。", sp.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            boolean keepTransform = player.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_KEEP_TRANSFORM);
            boolean resetHp = player.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_RESET_HP);

            if (!keepTransform) {
                trans.stopTransformation(player);
            }

            if (resetHp) {
                MonsterTransformUtil.resetPlayerHP(player);
                MonsterTransformUtil.resetIdentityHP(player);
            } else {
                if (keepTransform && trans.isTransformed()) {
                    String currentId = trans.getIdentity().getId();
                    double currentHP = MonsterTransformUtil.getIdentityHP(player, currentId);
                    if (currentHP <= 0) {
                        MonsterTransformUtil.setIdentityHP(player, currentId, MonsterTransformUtil.getIdentityMaxHP(player));
                    }
                } else if (MonsterTransformUtil.getPlayerHP(player) <= 0) {
                    MonsterTransformUtil.resetPlayerHP(player);
                }
            }

            // 属性の適用
            MonsterTransformUtil.applyFullTransformation(player, trans);

            double finalHP = trans.isTransformed()
                    ? MonsterTransformUtil.getIdentityHP(player, trans.getIdentity().getId())
                    : MonsterTransformUtil.getPlayerHP(player);

            if (finalHP <= 0) finalHP = player.getMaxHealth();
            player.setHealth((float) finalHP);

            if (player instanceof ServerPlayer sp) {
                // ★ 既存の syncToClient を使用。内部で S2CTransformSyncPacket が飛ぶ
                trans.syncToClient(sp);
                MonsterTransformUtil.saveAllToNBT(player);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        oldPlayer.reviveCaps();
        CapabilityRegistry.copyCaps(oldPlayer, newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // 1. 変身状態・HPはMonsterTransformation Capability(ICapabilitySerializable)により
        //    Forgeの標準プレイヤーロード処理で既に復元済み。Attributeスナップショットのみ別途復元する。
        MonsterTransformUtil.loadAllFromNBT(sp);

        sp.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // 2. deserializeNBTで復元済みのフラグ(isTransformed/mobId)を基に
            //    Entity/Identityインスタンスを再構築する(ID: null 解消)
            trans.onLoad(sp);

            // 3. ロードされたデータに基づき属性を再適用
            MonsterTransformUtil.applyFullTransformation(sp, trans);

            // 4. クライアント全体へ同期
            trans.syncToClient(sp);
            trans.syncToAllClients(sp);

            MonsterMod.LOGGER.info("[LoginLoad] {} transformation restored: {}", sp.getName().getString(), trans.isTransformed());
        });
    }
}