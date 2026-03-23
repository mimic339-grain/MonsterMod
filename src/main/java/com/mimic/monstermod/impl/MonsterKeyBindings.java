package com.mimic.monstermod.impl;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MonsterKeyBindings {

    public static final String KEY_CATEGORY_MONSTERMOD = "key.category." + MonsterMod.MOD_ID + ".monstermod";

    public static KeyMapping[] SKILL_KEYS = new KeyMapping[12];
    public static KeyMapping MENU_KEY;
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {

        int[] keyCodes = new int[]{
                GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U,
                GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_Z,
                GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B
        };

        for (int i = 0; i < 12; i++) {
            SKILL_KEYS[i] = new KeyMapping(
                    "key." + MonsterMod.MOD_ID + ".skill_" + (i + 1),
                    InputConstants.Type.KEYSYM,
                    keyCodes[i],
                    KEY_CATEGORY_MONSTERMOD
            );
            event.register(SKILL_KEYS[i]);
        }

        MENU_KEY = new KeyMapping(
                "key." + MonsterMod.MOD_ID + ".menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KEY_CATEGORY_MONSTERMOD
        );
        event.register(MENU_KEY);
    }
}