package com.mimic.monstermod;

import com.mimic.monstermod.command.ModCommands;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.ModEntityAttributes;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;

import com.mimic.monstermod.impl.ServerInputHandler;
import com.mimic.monstermod.item.ModItems;
import com.mimic.monstermod.network.ModMessages;
import com.mojang.logging.LogUtils;
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
    public static final Logger LOGGER = LogUtils.getLogger();

    // 通信チャンネル
    public static SimpleChannel CHANNEL;

    // ✅ サーバー入力ハンドラ（グローバルに管理）
    private static final ServerInputHandler SERVER_INPUT_HANDLER = new ServerInputHandler();

    public static Logger getLogger() {
        return LOGGER;
    }

    // ✅ 外部から参照可能
    public static ServerInputHandler getServerInputHandler() {
        return SERVER_INPUT_HANDLER;
    }

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 登録系
        ModEntitieType.register(modEventBus);
        ModItems.register(modEventBus);
        ModMessages.register();
        modEventBus.register(ModEntityAttributes.class);
        modEventBus.register(BaseMonsterIdentityRegistry.class);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
