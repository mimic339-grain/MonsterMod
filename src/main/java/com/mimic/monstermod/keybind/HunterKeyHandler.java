package com.mimic.monstermod.keybind;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SHunterInputPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HunterKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        handleInput();
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        handleInput();
    }

    private static void handleInput() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        mc.player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(hunter -> {
            if (!hunter.isActive()) return;

            // スキル 1-3
            for (int i = 0; i < HunterKeyBindings.SKILL_KEYS.length; i++) {
                if (HunterKeyBindings.SKILL_KEYS[i].consumeClick()) {
                    ModMessages.sendToServer(new C2SHunterInputPacket(i, false, false));
                }
            }

            // 回避
            if (HunterKeyBindings.DODGE_KEY.consumeClick()) {
                ModMessages.sendToServer(new C2SHunterInputPacket(3, true, false));
            }

            // 納刀
            if (HunterKeyBindings.SHEATH_KEY.consumeClick()) {
                ModMessages.sendToServer(new C2SHunterInputPacket(-1, false, true));
            }
        });
    }
}