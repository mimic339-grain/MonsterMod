package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.item.ModItems; // ModItemsをインポート
import net.mimic.monstermod.networking.ModMessages;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.mimic.monstermod.event.ModEvents;
import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
@EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus); // ModItemsの登録を追加
        ModMessages.register();

        modEventBus.register(ModEvents.class);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        net.mimic.monstermod.command.ModCommands.register(event.getDispatcher());
    }
}