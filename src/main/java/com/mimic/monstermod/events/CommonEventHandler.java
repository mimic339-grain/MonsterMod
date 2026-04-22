package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.skill.hunter.skills.LeapJumpSkill;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingFallEvent; // 変更
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class CommonEventHandler {

    /**
     * 着地した瞬間の落下距離計算イベント
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            // サーバー側で処理
            if (player.level().isClientSide) return;

            CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                // LeapJumpの保護フラグがあるかチェック
                if (cap.hasState(LeapJumpSkill.STATE_KEY)) {

                    // 1. イベントをキャンセル（これでダメージ計算と「赤い画面」が両方消える）
                    event.setCanceled(true);

                    // 2. 落下距離を0に上書き（念押し）
                    event.setDistance(0);

                    // 3. フラグを折る
                    cap.setState(LeapJumpSkill.STATE_KEY, false);
                }
            });
        }
    }
}