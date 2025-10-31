package com.mimic.monstermod.impl;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.client.MonsterKeyBindings;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SPlayerInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * ClientKeyHandler 完全版
 * - 攻撃キー廃止
 * - スキル1〜11、回避、メニューに対応
 * - 入力1回ごとにサーバーへ送信
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientKeyHandler {

    private static final Map<MonsterKeyBindings.KeyMappingWrapper, Boolean> keyStateMap = new HashMap<>();

    static {
        // すべてのスキルキー
        for (MonsterKeyBindings.KeyMappingWrapper wrapper : MonsterKeyBindings.getSkillWrappers()) {
            keyStateMap.put(wrapper, false);
        }

        // 回避・メニューキー
        keyStateMap.put(MonsterKeyBindings.DODGE_KEY_WRAPPER, false);
        keyStateMap.put(MonsterKeyBindings.MENU_KEY_WRAPPER, false);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    // スキルキー
                    for (int i = 0; i < MonsterKeyBindings.getSkillWrappers().size(); i++) {
                        MonsterKeyBindings.KeyMappingWrapper wrapper = MonsterKeyBindings.getSkillWrappers().get(i);
                        handleKey(player, wrapper, false, false, i);
                    }

                    // 回避キー
                    handleKey(player, MonsterKeyBindings.DODGE_KEY_WRAPPER, true, false, -1);

                    // メニューキー
                    handleKey(player, MonsterKeyBindings.MENU_KEY_WRAPPER, false, true, -1);
                });
    }

    private static void handleKey(LocalPlayer player,
                                  MonsterKeyBindings.KeyMappingWrapper wrapper,
                                  boolean dodgeKey,
                                  boolean menuKey,
                                  int skillIndex) {
        boolean pressed = wrapper.key.isDown();
        boolean wasPressed = keyStateMap.getOrDefault(wrapper, false);

        if (pressed && !wasPressed) {
            ModMessages.sendToServer(new C2SPlayerInputPacket(menuKey, dodgeKey, skillIndex));
        }

        keyStateMap.put(wrapper, pressed);
    }
}
