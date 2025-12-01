package com.mimic.monstermod.impl;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SMonsterInputPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonsterKeyHandler {

    // ======================================
    // キーバインディング登録
    // ======================================
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (var key : MonsterKeyBindings.SKILL_KEYS)
            event.register(key);

        event.register(MonsterKeyBindings.MENU_KEY);
        event.register(MonsterKeyBindings.DODGE_KEY);
    }

    // ======================================
    // キー入力チェック（毎Tick）
    // ======================================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {

                    // 変身していないなら何もしない
                    if (!trans.isTransformed()) return;

                    BaseMonsterIdentity identity = trans.getIdentity();
                    if (identity == null) return;

                    boolean dodgePressed = MonsterKeyBindings.DODGE_KEY.consumeClick();

                    // ---------- スキルキー処理 ----------
                    for (int i = 0; i < MonsterKeyBindings.SKILL_KEYS.length; i++) {
                        if (MonsterKeyBindings.SKILL_KEYS[i].consumeClick()) {

                            // クライアント側アクション
                            identity.handleClientInput(player, true, false, i);

                            // サーバーへ送信
                            ModMessages.INSTANCE.sendToServer(
                                    new C2SMonsterInputPacket(true, false, dodgePressed, i)
                            );
                        }
                    }

                    // ---------- メニューキー処理 ----------
                    if (MonsterKeyBindings.MENU_KEY.consumeClick()) {

                        identity.handleClientInput(player, false, true, -1);

                        ModMessages.INSTANCE.sendToServer(
                                new C2SMonsterInputPacket(false, true, dodgePressed, -1)
                        );
                    }

                    // ---------- 回避キー単独処理 ----------
                    if (dodgePressed) {
                        identity.handleDodge(player);
                        ModMessages.INSTANCE.sendToServer(
                                new C2SMonsterInputPacket(false, false, true, -1)
                        );
                    }
                });

    }
}
