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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // ENDフェーズで処理（移動などが確定した後）
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;

        // Capability経由でIdentityを取得してTickを回す
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            var identity = trans.getIdentity();
            if (identity != null) {
                if (player.level().isClientSide()) {
                    identity.tickClient(player); // クールダウン減算 + アニメラップ
                } else {
                    identity.tickServer(player); // クールダウン減算 + 同期
                }
            }
        });
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        // SkillUtil（サーバー側のスキル進行・ダメージ判定）を回す
        for (ServerLevel level : event.getServer().getAllLevels()) {
            SkillUtil.tick(level);
        }
    }
}