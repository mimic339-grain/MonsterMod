package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MonsterMod KeyBindings 完全版（AttackKey 削除版）
 * - スキルキー 1〜11 に統合
 * - 回避 / メニューキーも登録
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MonsterKeyBindings {

    // -----------------------------
    // 基本キー（AttackKey 削除）
    // -----------------------------
    public static final KeyMapping DODGE_KEY =
            new KeyMapping("key.monstermod.dodge", GLFW.GLFW_KEY_LEFT_SHIFT, "key.category.monstermod");
    public static final KeyMapping MENU_KEY =
            new KeyMapping("key.monstermod.open_menu", GLFW.GLFW_KEY_M, "key.category.monstermod");

    // -----------------------------
    // スキルキー 1〜11
    // -----------------------------
    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[]{
            new KeyMapping("key.monstermod.skill_1", GLFW.GLFW_KEY_R, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_2", GLFW.GLFW_KEY_T, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_3", GLFW.GLFW_KEY_Y, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_4", GLFW.GLFW_KEY_U, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_5", GLFW.GLFW_KEY_I, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_6", GLFW.GLFW_KEY_O, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_7", GLFW.GLFW_KEY_P, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_8", GLFW.GLFW_KEY_H, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_9", GLFW.GLFW_KEY_J, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_10", GLFW.GLFW_KEY_K, "key.category.monstermod"),
            new KeyMapping("key.monstermod.skill_11", GLFW.GLFW_KEY_L, "key.category.monstermod")
    };

    // -----------------------------
    // ClientKeyHandler 用ラッパー
    // -----------------------------
    public static class KeyMappingWrapper {
        public final KeyMapping key;
        public final int skillIndex; // スキルキーなら 0〜10、その他は -1

        public KeyMappingWrapper(KeyMapping key, int skillIndex) {
            this.key = key;
            this.skillIndex = skillIndex;
        }
    }

    public static final List<KeyMappingWrapper> SKILL_WRAPPERS;
    public static final KeyMappingWrapper DODGE_KEY_WRAPPER;
    public static final KeyMappingWrapper MENU_KEY_WRAPPER;

    static {
        List<KeyMappingWrapper> tmp = new ArrayList<>();
        for (int i = 0; i < SKILL_KEYS.length; i++) tmp.add(new KeyMappingWrapper(SKILL_KEYS[i], i));
        SKILL_WRAPPERS = Collections.unmodifiableList(tmp);

        DODGE_KEY_WRAPPER = new KeyMappingWrapper(DODGE_KEY, -1);
        MENU_KEY_WRAPPER = new KeyMappingWrapper(MENU_KEY, -1);
    }

    // -----------------------------
    // KeyMapping 登録
    // -----------------------------
    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DODGE_KEY);
        event.register(MENU_KEY);
        for (KeyMapping key : SKILL_KEYS) event.register(key);
    }

    public static List<KeyMappingWrapper> getSkillWrappers() {
        return SKILL_WRAPPERS;
    }
}
