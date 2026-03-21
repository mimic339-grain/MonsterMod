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

    // 1. データをここに隠す (ClientRootHandlerから移動)
    private static final Map<UUID, Integer> ROOT_MAP = new HashMap<>();

    // 2. パケットから呼ばれる窓口
    public static void applyRoot(UUID playerId, int durationTicks) {
        if (durationTicks <= 0) {
            ROOT_MAP.remove(playerId);
        } else {
            ROOT_MAP.put(playerId, durationTicks);
        }
    }

    // 3. 入力を物理的に封じる (ClientTickEventsから移動)
    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Integer ticks = ROOT_MAP.get(mc.player.getUUID());
        if (ticks != null && ticks > 0) {
            var input = event.getInput();
            input.leftImpulse = 0;
            input.forwardImpulse = 0;
            input.up = input.down = input.left = input.right = false;
            input.jumping = input.shiftKeyDown = false;
        }
    }

    // 4. 時間を進める & 慣性を殺す (ClientRootHandler.tickから移動)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || ROOT_MAP.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ROOT_MAP.entrySet().removeIf(entry -> {
            int ticksLeft = entry.getValue();
            if (ticksLeft <= 0) return true;

            // 自分の物理慣性を止める
            if (entry.getKey().equals(mc.player.getUUID())) {
                mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
            }

            entry.setValue(ticksLeft - 1);
            return false;
        });
    }
}