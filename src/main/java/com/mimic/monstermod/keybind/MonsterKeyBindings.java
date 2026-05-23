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
    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[12];
    public static KeyMapping MENU_KEY;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // マウスボタンをデフォルトにするための設定例
        // GLFW_MOUSE_BUTTON_1(左), 2(右), 3(中), 4(サイド後), 5(サイド前)
        // 注意: マイクラ標準機能と被る場合は、設定画面でプレイヤーに変更してもらう前提です
        int[] keyCodes = new int[]{
                GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, // 上段 1-6
                GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N  // 下段 7-12
        };

        for (int i = 0; i < 12; i++) {
            // Typeを指定しない通常のコンストラクタは、設定画面でマウス入力を受け付けます
            SKILL_KEYS[i] = new KeyMapping(
                    "key." + MonsterMod.MOD_ID + ".skill_" + (i + 1),
                    keyCodes[i],
                    KEY_CATEGORY_MONSTERMOD
            );
            event.register(SKILL_KEYS[i]);
        }

        MENU_KEY = new KeyMapping("key." + MonsterMod.MOD_ID + ".menu", GLFW.GLFW_KEY_M, KEY_CATEGORY_MONSTERMOD);
        event.register(MENU_KEY);
    }
}