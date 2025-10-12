package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.mimic.monstermod.entity.ModEntitieType;
import net.mimic.monstermod.entity.ModEntityAttributes;
import net.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import net.mimic.monstermod.item.ModItems;
import net.mimic.monstermod.networking.ModMessages;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 追加: ネットワーク通信チャンネル
    public static SimpleChannel CHANNEL;

    public static Logger getLogger() {
        return LOGGER;
    }

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // エンティティ・アイテム・メッセージ登録
        ModEntitieType.register(modEventBus);
        ModItems.register(modEventBus);
        ModMessages.register(); // ここで CHANNEL を初期化

        // イベント登録
        modEventBus.register(ModEntityAttributes.class);
        modEventBus.register(BaseMonsterIdentityRegistry.class);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        net.mimic.monstermod.command.ModCommands.register(event.getDispatcher());
    }
}
