package com.mimic.monstermod.keybind;

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
        // MonsterKeyBindingsで定義したスキルキーを全て登録
        for (var key : MonsterKeyBindings.SKILL_KEYS) {
            if (key != null) event.register(key);
        }
        // メニューキーを登録
        if (MonsterKeyBindings.MENU_KEY != null) {
            event.register(MonsterKeyBindings.MENU_KEY);
        }
    }

    // ======================================
    // キー入力チェック（毎Tick）
    // ======================================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Tickの終了タイミングで処理（重複防止）
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

                    // ---------- スキルキー処理 (R, T, Y, U...) ----------
                    for (int i = 0; i < MonsterKeyBindings.SKILL_KEYS.length; i++) {
                        if (MonsterKeyBindings.SKILL_KEYS[i].consumeClick()) {

                            // クライアント側での先行処理（予兆表示など）
                            identity.handleClientInput(player, true, false, i);

                            // サーバーへパケット送信
                            // 修正：Dodgeフラグを削除した新しいコンストラクタに対応
                            ModMessages.INSTANCE.sendToServer(
                                    new C2SMonsterInputPacket(true, false, i)
                            );
                        }
                    }

                    // ---------- メニューキー処理 (M) ----------
                    if (MonsterKeyBindings.MENU_KEY.consumeClick()) {

                        identity.handleClientInput(player, false, true, -1);

                        // サーバーへパケット送信
                        ModMessages.INSTANCE.sendToServer(
                                new C2SMonsterInputPacket(false, true, -1)
                        );
                    }
                });
    }
}