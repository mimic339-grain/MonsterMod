package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.skill.SkillUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public final class CommonTickEvents {

    /**
     * サーバー全体のTick
     * 1サーバーにつき1回だけ呼ばれるので、タスク管理や全ワールドのスキル処理に最適
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 1. 遅延タスクの進行（サーバーで1回だけ実行）
        com.mimic.monstermod.util.DelayUtil.tickDelayedTasks();

        // 2. 全ワールドのスキル処理（全ディメンションの時計を動かす）
        if (event.getServer() == null) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            SkillUtil.tick(level);
        }
    }

    /**
     * プレイヤー個別のTick
     * ハンター変身を優先しつつ、クールダウンや同期を行う
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;

        // モンスター変身中のみIdentityを進める
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.getIdentity() != null) {
                tickIdentity(player, trans.getIdentity());
            }
        });

        // 変身中プレイヤーの部位当たり判定を更新(クライアント/サーバー両方で必要)。
        // 見た目用プロキシへ移動量・向きがコピーされた後に呼ぶ必要があるため、
        // 必ず上のtickIdentityより後に置くこと。
        com.mimic.monstermod.entity.hitbox.TransformedPlayerHitboxes.tick(player);
    }

    /**
     * サイド（クライアント/サーバー）に応じたIdentityの更新処理
     */
    private static void tickIdentity(Player player, com.mimic.monstermod.identity.BaseIdentity identity) {
        if (player.level().isClientSide()) {
            identity.tickClient(player); // クールダウン減算 + アニメーション更新
        } else {
            identity.tickServer(player); // クールダウン減算 + サーバー側同期
        }
    }
}