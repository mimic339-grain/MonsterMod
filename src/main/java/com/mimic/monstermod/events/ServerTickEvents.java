package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.skill.SkillUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public final class ServerTickEvents {

    private ServerTickEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // START ではなく END に変更することで、その tick の全移動が終わった後に判定を行う
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            SkillUtil.tick(level);
        }
    }
}