package com.mimic.monstermod.keybind;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HunterKeyBindings {

    public static final String KEY_CATEGORY_HUNTER = "key.category." + MonsterMod.MOD_ID + ".hunter";

    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[3];
    public static KeyMapping DODGE_KEY;
    public static KeyMapping SHEATH_KEY;

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // --- Skill Keys ---
        // デフォルトをあえて UNKNOWN にすれば「最初は何も設定されていない」状態にできます
        // マウスのサイドボタンは GLFW.GLFW_MOUSE_BUTTON_4 や 5 です

        SKILL_KEYS[0] = createKey("hunter_skill_1", GLFW.GLFW_KEY_R);
        SKILL_KEYS[1] = createKey("hunter_skill_2", GLFW.GLFW_KEY_T);
        SKILL_KEYS[2] = createKey("hunter_skill_3", GLFW.GLFW_KEY_Y);

        // --- Dodge ---
        DODGE_KEY = createKey("hunter_dodge", GLFW.GLFW_KEY_L);

        // --- Sheath ---
        SHEATH_KEY = createKey("hunter_sheath", GLFW.GLFW_KEY_Q);

        // まとめて登録
        for (KeyMapping key : SKILL_KEYS) event.register(key);
        event.register(DODGE_KEY);
        event.register(SHEATH_KEY);
    }

    private static KeyMapping createKey(String name, int defaultKey) {
        // InputConstants.Type.MOUSE ならマウス専用になりますが、
        // バニラの KeyMapping は賢いので、デフォルトがキーボードでも
        // 設定画面でマウスボタンを押せば自動的にマウスボタンとして保存されます。
        return new KeyMapping(
                "key." + MonsterMod.MOD_ID + "." + name,
                defaultKey, // 初期キー
                KEY_CATEGORY_HUNTER
        );
    }
}