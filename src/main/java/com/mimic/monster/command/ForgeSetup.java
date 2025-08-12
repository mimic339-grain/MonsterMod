package com.mimic.monster.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
        modid = "monstermod",
        bus = Bus.FORGE
)
public class ForgeSetup {
    @SubscribeEvent
    //RegisterCommandsEvent はサーバー側のコマンド登録イベント
    public static void serverStarting(RegisterCommandsEvent event) {
        //dispatcherでコマンドをTABにするもの
        CommandDispatcher dispatcher = event.getDispatcher();
        TransformCommand.register(dispatcher);
    }
}
