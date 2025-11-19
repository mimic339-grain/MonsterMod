package com.mimic.monstermod.events;

import com.mimic.monstermod.util.MonsterTransformUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ====================================
// Player / Identity HPMap 更新イベント
// ====================================
@Mod.EventBusSubscriber
public class PlayerHPEvents {

    // ------------------------
    // ダメージを受けたとき
    // ------------------------
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double damage = event.getAmount();

        // 変身中なら IdentityHP を更新
        player.getCapability(com.mimic.monstermod.capability.PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(cap -> {
                    if (cap.isTransformed() && cap.getIdentity() != null) {
                        String identityId = cap.getIdentity().getId();
                        MonsterTransformUtil.damageIdentity(player, identityId, damage);
                    } else {
                        MonsterTransformUtil.damagePlayer(player, damage);
                    }
                });
    }

    // ------------------------
    // 自然回復 / 回復アイテム
    // ------------------------
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double healAmount = event.getAmount();

        // Capability を取得して処理
        player.getCapability(com.mimic.monstermod.capability.PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getIdentity() != null) {
                        // 変身中なら変身中の IdentityHP に回復
                        String identityId = transformation.getIdentity().getId();
                        double newHP = MonsterTransformUtil.getIdentityHP(player, identityId) + healAmount;
                        MonsterTransformUtil.setIdentityHP(player, identityId, newHP);
                    } else {
                        // 変身していない場合は PlayerHP に回復
                        double newHP = MonsterTransformUtil.getPlayerHP(player) + healAmount;
                        MonsterTransformUtil.setPlayerHP(player, newHP);
                    }
                });
    }

    // ------------------------
    // 死亡時
    // ------------------------
    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();

        // PlayerHP を最大値にリセット
        MonsterTransformUtil.resetPlayerHP(player);

        // 変身中の IdentityHP も最大値にリセット
        MonsterTransformUtil.resetIdentityHP(player);

        // NBT に保存
        MonsterTransformUtil.saveAllToNBT(player);
    }

    // ------------------------
    // プレイヤークローン時（死亡・リスポーン）
    // ------------------------
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        // PlayerHP をコピー
        double oldPlayerHP = MonsterTransformUtil.getPlayerHP(oldPlayer);
        MonsterTransformUtil.setPlayerHP(newPlayer, oldPlayerHP);

        // IdentityHP をコピー（変身中の Identity のみ）
        oldPlayer.getCapability(com.mimic.monstermod.capability.PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(cap -> {
                    if (cap.getIdentity() != null) {
                        String identityId = cap.getIdentity().getId();
                        double oldIdentityHP = MonsterTransformUtil.getIdentityHP(oldPlayer, identityId);
                        MonsterTransformUtil.setIdentityHP(newPlayer, identityId, oldIdentityHP);
                    }
                });

        // NBT もコピー
        MonsterTransformUtil.loadAllFromNBT(oldPlayer);
        MonsterTransformUtil.saveAllToNBT(newPlayer);
    }
}
