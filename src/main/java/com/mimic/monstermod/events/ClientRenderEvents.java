package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class ClientRenderEvents {

    //変身中の腕見せ禁止
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // 変身中かどうかをCapabilityでチェック
        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed()) {
                // ★ バニラの腕の描画をキャンセル！これでスキンが見えなくなります
                event.setCanceled(true);
            }
        });
    }
}