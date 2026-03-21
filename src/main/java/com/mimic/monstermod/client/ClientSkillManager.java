package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSkillManager {

    private static final Map<UUID, Integer> ROOT_MAP = new HashMap<>();

    /**
     * サーバーからの命令を受信
     * @param durationTicks 0より大きければ停止、0なら解除
     */
    public static void applyRoot(UUID playerId, int durationTicks) {
        if (durationTicks <= 0) {
            ROOT_MAP.remove(playerId);
        } else {
            // クライアント側ではカウントダウンせず、単なる「停止フラグ」として保持
            ROOT_MAP.put(playerId, durationTicks);
        }
    }

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // ROOT_MAPにデータが存在する間は入力をキャンセルし続ける
        if (ROOT_MAP.containsKey(mc.player.getUUID())) {
            var input = event.getInput();
            input.leftImpulse = 0;
            input.forwardImpulse = 0;
            input.up = input.down = input.left = input.right = false;
            input.jumping = input.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || ROOT_MAP.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 自分の物理慣性（滑り）のみを止める
        // カウントダウン処理(ticksLeft-1)はサーバーに任せるため削除
        if (ROOT_MAP.containsKey(mc.player.getUUID())) {
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
        }
    }
}