package com.mimic.monstermod.impl;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SHunterInputPacket;
import com.mimic.monstermod.util.HunterUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HunterKeyHandler
 * クライアント側でのキー押下検知およびサーバー送信
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public class HunterKeyHandler {

    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.player == null || mc.level == null) return;

        LocalPlayer player = mc.player;
        HunterTransformation hunter = HunterUtil.getHunter(player);

        if (hunter == null || !hunter.isActive()) return; // 変身中のみ処理

        // ===========================
        // Skill1/2/3
        // ===========================
        for (int i = 0; i < HunterKeyBindings.SKILL_KEYS.length; i++) {
            if (HunterKeyBindings.SKILL_KEYS[i].consumeClick()) {
                // サーバーへ送信
                ModMessages.sendToServer(new C2SHunterInputPacket(
                        i + 1, // skill slot 1~3
                        false,  // DODGE
                        false,  // SHEATH
                        false   // MENU
                ));
            }
        }

        // ===========================
        // Dodge
        // ===========================
        if (HunterKeyBindings.DODGE_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SHunterInputPacket(
                    0,      // skill slotなし
                    true,   // DODGE
                    false,  // SHEATH
                    false   // MENU
            ));
        }

        // ===========================
        // Sheath (抜刀/納刀)
        // ===========================
        if (HunterKeyBindings.SHEATH_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SHunterInputPacket(
                    0,      // skill slotなし
                    false,  // DODGE
                    true,   // SHEATH
                    false   // MENU
            ));
        }

        // ===========================
        // Menu
        // ===========================
        if (HunterKeyBindings.MENU_KEY.consumeClick()) {
            ModMessages.sendToServer(new C2SHunterInputPacket(
                    0,      // skill slotなし
                    false,  // DODGE
                    false,  // SHEATH
                    true    // MENU
            ));
        }
    }
}
