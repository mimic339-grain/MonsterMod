package com.mimic.monstermod.impl;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SHunterInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HunterKeyHandler
 * クライアント側でのキー押下検知およびサーバー送信
 *
 * 重要：
 * - メニューキーは存在しない
 * - インベントリ（Eキー）＝ Hunterメニュー
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class HunterKeyHandler {

    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // ===========================
        // Skill1 / Skill2 / Skill3
        // ===========================
        for (int i = 0; i < HunterKeyBindings.SKILL_KEYS.length; i++) {
            if (HunterKeyBindings.SKILL_KEYS[i].consumeClick()) {
                ModMessages.sendToServer(new C2SHunterInputPacket(
                        i + 1, // skill slot 1~3
                        false, // DODGE
                        false  // SHEATH
                ));
            }
        }

        // ===========================
        // Dodge
        // ===========================
        if (HunterKeyBindings.DODGE_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SHunterInputPacket(
                    0,
                    true,  // DODGE
                    false  // SHEATH
            ));
        }

        // ===========================
        // Sheath（抜刀 / 納刀）
        // ===========================
        if (HunterKeyBindings.SHEATH_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SHunterInputPacket(
                    0,
                    false, // DODGE
                    true   // SHEATH
            ));
        }
    }
}
