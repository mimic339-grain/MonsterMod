package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.mimic.monstermod.client.renderer.identity.PlayerIdentityRenderer;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.event.ModEvents;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.mimic.monstermod.item.ModItems;
import net.mimic.monstermod.networking.ModMessages;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Logger getLogger() {
        return LOGGER;
    }

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMessages.register();

        modEventBus.register(ModEvents.class);
        modEventBus.register(PlayerIdentityRegistry.class);
        modEventBus.register(PlayerIdentityRenderer.class);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        net.mimic.monstermod.command.ModCommands.register(event.getDispatcher());
    }
}