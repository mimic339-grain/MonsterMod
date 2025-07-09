package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.mimic.monstermod.command.ModCommands;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.networking.ModMessages;
import net.minecraftforge.event.RegisterCommandsEvent; // 追加
import net.minecraftforge.eventbus.api.SubscribeEvent; // 追加
import net.minecraftforge.fml.common.Mod.EventBusSubscriber; // 追加

import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
// Forgeイベントバスを購読するために追加
@EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModMessages.register();
    }

    // ★追加: コマンド登録イベントハンドラ★
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}