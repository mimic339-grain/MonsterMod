package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class PlayerEvents {

    // ================================
    // ダメージ処理（Player / IdentityHP 更新）
    // ================================
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double damage = event.getAmount();

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(cap -> {
                    if (cap.isTransformed() && cap.getIdentity() != null) {
                        String identityId = cap.getIdentity().getId();
                        MonsterTransformUtil.damageIdentity(player, identityId, damage);
                    } else {
                        MonsterTransformUtil.damagePlayer(player, damage);
                    }
                });
    }

    // ================================
    // 回復処理（Player / IdentityHP 更新）
    // ================================
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double healAmount = event.getAmount();

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(cap -> {
                    if (cap.isTransformed() && cap.getIdentity() != null) {
                        String identityId = cap.getIdentity().getId();
                        double newHP = MonsterTransformUtil.getIdentityHP(player, identityId) + healAmount;
                        MonsterTransformUtil.setIdentityHP(player, identityId, newHP);
                    } else {
                        double newHP = MonsterTransformUtil.getPlayerHP(player) + healAmount;
                        MonsterTransformUtil.setPlayerHP(player, newHP);
                    }
                });
    }

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

        // 古いプレイヤーのNBTからHPデータを取得
        CompoundTag oldTag = oldPlayer.getPersistentData().getCompound("monster_transform_hp");

        // NBTを新しいプレイヤーにロード
        MonsterTransformUtil.loadHPFromNBT(newPlayer, oldTag);

        // 新しいプレイヤーのNBTに保存（更新）
        CompoundTag newTag = new CompoundTag();
        MonsterTransformUtil.saveHPToNBT(newPlayer, newTag);
        newPlayer.getPersistentData().put("monster_transform_hp", newTag);
    }

    // ================================
    // リスポーン時（死亡後含む）
    // ================================
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Capability を同期
        CapabilityRegistry.syncToClient(player);

        // 死亡時に 0 になった IdentityHP を安全にリセット
        MonsterTransformUtil.resetRespawnIdentityHP(player);

        // PlayerHP を最大値にリセット（死亡リスポーン時のみ）
        MonsterTransformUtil.resetPlayerHP(player);

        // NBT にも保存
        CompoundTag tag = player.getPersistentData().getCompound("monster_transform_hp");
        MonsterTransformUtil.saveHPToNBT(player, tag);
        player.getPersistentData().put("monster_transform_hp", tag);
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
    // ディメンション移動時
    // ================================
    @SubscribeEvent
    public static void onPlayerDimChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CapabilityRegistry.syncToClient(serverPlayer);
        }
    }
}
