package com.mimic.monstermod.command;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModSystemManager {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 食料システム一括切り替え
        event.getDispatcher().register(
                Commands.literal("monstermod_food")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    // 全プレイヤーに対して処理
                                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                        player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
                                            cap.setState(IPlayerData.STATE_HIDE_FOOD, !enabled);
                                            CapabilityRegistry.syncToClient(player); // 確実な同期
                                        });
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("サーバー全体の食料システムを " + (enabled ? "有効" : "無効") + " にしました。"), true);
                                    return 1;
                                }))
        );

        // アーマー表示一括切り替え
        event.getDispatcher().register(
                Commands.literal("monstermod_armor")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("visible", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean visible = BoolArgumentType.getBool(context, "visible");
                                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                        player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
                                            cap.setState(IPlayerData.STATE_HIDE_ARMOR, !visible);
                                            CapabilityRegistry.syncToClient(player);
                                        });
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("サーバー全体のアーマー表示を " + (visible ? "表示" : "非表示") + " にしました。"), true);
                                    return 1;
                                }))
        );
    }
}