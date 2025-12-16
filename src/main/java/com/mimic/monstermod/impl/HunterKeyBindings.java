package com.mimic.monstermod.impl;

import com.mimic.monstermod.MonsterMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HunterKeyBindings {

    public static final String KEY_CATEGORY_HUNTER =
            "key.category." + MonsterMod.MOD_ID + ".hunter";

    // ===========================
    // Skill1 / Skill2 / Skill3
    // ===========================
    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[3];

    // ===========================
    // Dodge / Sheath
    // ===========================
    public static KeyMapping DODGE_KEY;
    public static KeyMapping SHEATH_KEY;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {

        // ---------------------------
        // Skill keys
        // ---------------------------
        int[] skillKeyCodes = {
                GLFW.GLFW_KEY_R, // Skill1
                GLFW.GLFW_KEY_T, // Skill2
                GLFW.GLFW_KEY_Y  // Skill3
        };

        for (int i = 0; i < 3; i++) {
            SKILL_KEYS[i] = new KeyMapping(
                    "key." + MonsterMod.MOD_ID + ".hunter_skill_" + (i + 1),
                    InputConstants.Type.KEYSYM,
                    skillKeyCodes[i],
                    KEY_CATEGORY_HUNTER
            );
            event.register(SKILL_KEYS[i]);
        }

        // ---------------------------
        // Dodge
        // ---------------------------
        DODGE_KEY = new KeyMapping(
                "key." + MonsterMod.MOD_ID + ".hunter_dodge",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                KEY_CATEGORY_HUNTER
        );
        event.register(DODGE_KEY);

        // ---------------------------
        // Sheath
        // ---------------------------
        SHEATH_KEY = new KeyMapping(
                "key." + MonsterMod.MOD_ID + ".hunter_sheath",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Q,
                KEY_CATEGORY_HUNTER
        );
        event.register(SHEATH_KEY);
    }
}
