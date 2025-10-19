package com.mimic.monstermod.impl;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.C2SPlayerSkillInputPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientKeyHandler {

    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[]{
            new KeyMapping("key.monstermod.skill_1", GLFW.GLFW_KEY_R, "key.categories.gameplay"),
            new KeyMapping("key.monstermod.skill_2", GLFW.GLFW_KEY_T, "key.categories.gameplay"),
            new KeyMapping("key.monstermod.skill_3", GLFW.GLFW_KEY_Y, "key.categories.gameplay")
    };

    public static final KeyMapping MENU_KEY = new KeyMapping(
            "key.monstermod.open_menu", GLFW.GLFW_KEY_M, "key.categories.gameplay"
    );

    private static final Map<KeyMapping, Boolean> keyStateMap = new HashMap<>();

    static {
        for (KeyMapping key : SKILL_KEYS) keyStateMap.put(key, false);
        keyStateMap.put(MENU_KEY, false);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // スキルキー処理
        for (int i = 0; i < SKILL_KEYS.length; i++) handleSkillKey(player, SKILL_KEYS[i], i);

        // メニューキー処理
        handleMenuKey(player, MENU_KEY);
    }

    private static void handleSkillKey(LocalPlayer player, KeyMapping key, int skillIndex) {
        boolean pressed = key.isDown();
        boolean wasPressed = keyStateMap.getOrDefault(key, false);
        if (pressed && !wasPressed) {
            // クライアント側で演出やUI反映はここで可能
            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        BaseMonsterIdentity identity = trans.getIdentity();
                        if (identity != null) identity.handleClientInput(player, true, false, skillIndex);
                        // サーバーへ送信
                        ModMessages.INSTANCE.sendToServer(new C2SPlayerSkillInputPacket(skillIndex));
                    });
        }
        keyStateMap.put(key, pressed);
    }

    private static void handleMenuKey(LocalPlayer player, KeyMapping key) {
        boolean pressed = key.isDown();
        boolean wasPressed = keyStateMap.getOrDefault(key, false);
        if (pressed && !wasPressed) {
            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        BaseMonsterIdentity identity = trans.getIdentity();
                        if (identity != null) identity.handleClientInput(player, false, true, -1);
                        ModMessages.INSTANCE.sendToServer(new C2SPlayerSkillInputPacket(-1));
                    });
        }
        keyStateMap.put(key, pressed);
    }
}
