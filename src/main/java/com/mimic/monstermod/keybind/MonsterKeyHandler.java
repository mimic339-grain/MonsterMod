package com.mimic.monstermod.keybind;

import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2SMonsterInputPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonsterKeyHandler {

    // マウス入力イベントを監視
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        processInputs();
    }

    // キーボード入力イベントを監視
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        processInputs();
    }

    // クライアントTickでも念のためチェック（長押しや取りこぼし対策）
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processInputs();
        }
    }

    private static void processInputs() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // 画面が開いている時やプレイヤーがいない時は処理しない
        if (player == null || mc.screen != null) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (!trans.isTransformed()) return;

            BaseIdentity identity = trans.getIdentity();
            if (identity == null) return;

            // スキルキーの判定
            for (int i = 0; i < MonsterKeyBindings.SKILL_KEYS.length; i++) {
                // consumeClick() を使うことで、バニラや他MODにこの入力を渡さないようにする
                if (MonsterKeyBindings.SKILL_KEYS[i].consumeClick()) {
                    identity.handleClientInput(player, true, false, i);
                    ModMessages.INSTANCE.sendToServer(new C2SMonsterInputPacket(true, false, i));
                }
            }

            // メニューキーの判定
            if (MonsterKeyBindings.MENU_KEY.consumeClick()) {
                identity.handleClientInput(player, false, true, -1);
                ModMessages.INSTANCE.sendToServer(new C2SMonsterInputPacket(false, true, -1));
            }
        });
    }
}