package com.mimic.monstermod.impl;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SPlayerInputPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientInputHandler {

    // -----------------------------
    // キー設定
    // -----------------------------
    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[]{
            new KeyMapping("key.monstermod.skill_1", GLFW.GLFW_KEY_R, "key.categories.gameplay"),
            new KeyMapping("key.monstermod.skill_2", GLFW.GLFW_KEY_T, "key.categories.gameplay"),
            new KeyMapping("key.monstermod.skill_3", GLFW.GLFW_KEY_Y, "key.categories.gameplay")
    };

    public static final KeyMapping MENU_KEY = new KeyMapping(
            "key.monstermod.open_menu", GLFW.GLFW_KEY_M, "key.categories.gameplay"
    );

    private static final Map<KeyMapping, Boolean> keyStateMap = new HashMap<>();

    // 移動入力差分保持
    private static float lastForward = 0;
    private static float lastStrafe = 0;
    private static boolean lastJump = false;
    private static boolean lastSprint = false;

    static {
        for (KeyMapping key : SKILL_KEYS) keyStateMap.put(key, false);
        keyStateMap.put(MENU_KEY, false);
    }

    // -----------------------------
    // KeyInput: スキル・メニューキー
    // -----------------------------
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // スキルキー押下
        for (int i = 0; i < SKILL_KEYS.length; i++) handleSkillKey(player, SKILL_KEYS[i], i);
        // メニューキー押下
        handleMenuKey(player, MENU_KEY);
    }

    private static void handleSkillKey(LocalPlayer player, KeyMapping key, int skillIndex) {
        boolean pressed = key.isDown();
        boolean wasPressed = keyStateMap.getOrDefault(key, false);
        if (pressed && !wasPressed) {
            // クライアントは押下通知のみ送信
            ModMessages.INSTANCE.sendToServer(new C2SPlayerInputPacket(true, false, skillIndex));
        }
        keyStateMap.put(key, pressed);
    }

    private static void handleMenuKey(LocalPlayer player, KeyMapping key) {
        boolean pressed = key.isDown();
        boolean wasPressed = keyStateMap.getOrDefault(key, false);
        if (pressed && !wasPressed) {
            ModMessages.INSTANCE.sendToServer(new C2SPlayerInputPacket(false, true, -1));
        }
        keyStateMap.put(key, pressed);
    }

    // -----------------------------
    // PlayerTick: 移動・ジャンプ・スプリント
    // -----------------------------
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Player player = event.player;
        if (!(player instanceof LocalPlayer)) return;

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    BaseMonsterEntity entity = trans.getEntity();
                    if (entity == null) return;

                    Minecraft mc = Minecraft.getInstance();
                    float forward = mc.options.keyUp.isDown() ? 1 : mc.options.keyDown.isDown() ? -1 : 0;
                    float strafe = mc.options.keyLeft.isDown() ? -1 : mc.options.keyRight.isDown() ? 1 : 0;
                    boolean jump = mc.options.keyJump.isDown();
                    boolean sprint = mc.options.keySprint.isDown();

                    boolean changed = (forward != lastForward) || (strafe != lastStrafe)
                            || (jump != lastJump) || (sprint != lastSprint);

                    if (changed) {
                        // クライアント側反映
                        entity.moveRelative(forward, strafe);
                        if (jump) entity.jumpFromGround();
                        entity.setSprinting(sprint);
                        entity.setPlayerActiveMove(forward != 0 || strafe != 0 || jump);

                        // サーバー送信（移動差分）
                        ModMessages.INSTANCE.sendToServer(
                                new C2SPlayerInputPacket(forward, strafe, jump, sprint)
                        );

                        lastForward = forward;
                        lastStrafe = strafe;
                        lastJump = jump;
                        lastSprint = sprint;
                    }
                });
    }
}
