package com.mimic.monstermod.keybind;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MonsterKeyBindings {

    public static final String KEY_CATEGORY_MONSTERMOD = "key.category." + MonsterMod.MOD_ID + ".monstermod";

    // スキルキー12個 + メニューキー
    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[12];
    public static KeyMapping MENU_KEY;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // デフォルトのキー配列
        int[] keyCodes = new int[]{
                GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U,
                GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_Z,
                GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B
        };

        for (int i = 0; i < 12; i++) {
            // 第2引数に InputConstants.Type を指定しないことで、マウス入力も受け入れ可能にする
            SKILL_KEYS[i] = new KeyMapping(
                    "key." + MonsterMod.MOD_ID + ".skill_" + (i + 1),
                    keyCodes[i],
                    KEY_CATEGORY_MONSTERMOD
            );
            event.register(SKILL_KEYS[i]);
        }

        MENU_KEY = new KeyMapping(
                "key." + MonsterMod.MOD_ID + ".menu",
                GLFW.GLFW_KEY_M,
                KEY_CATEGORY_MONSTERMOD
        );
        event.register(MENU_KEY);
    }
}