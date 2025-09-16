package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.item.ModItems;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.event.CapabilityEvents;
import net.mimic.monstermod.command.ModCommands;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Logger getLogger() { return LOGGER; }

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);

        ModMessages.register();

        // CapabilityEventsはForgeバスに登録
        CapabilityEvents.register();

        // Forgeバスに自身を登録
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering transform command...");
        ModCommands.register(event.getDispatcher());
    }

    public static EntityType<?> getMimicEntityType() {
        return ModEntities.MIMIC.get();
    }
}
