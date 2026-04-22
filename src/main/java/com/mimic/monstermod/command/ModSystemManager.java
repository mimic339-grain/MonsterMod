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
        // 食料システムコマンド
        event.getDispatcher().register(
                Commands.literal("monstermod_food")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
                                        cap.setState(IPlayerData.STATE_HIDE_FOOD, !enabled);
                                        String status = enabled ? "有効" : "無効";
                                        context.getSource().sendSuccess(() -> Component.literal("食料システムを " + status + " にしました。"), true);
                                    });
                                    return 1;
                                }))
        );

        // アーマー表示コマンド (新規追加)
        event.getDispatcher().register(
                Commands.literal("monstermod_armor")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("visible", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean visible = BoolArgumentType.getBool(context, "visible");
                                    player.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
                                        cap.setState(IPlayerData.STATE_HIDE_ARMOR, !visible);
                                        String status = visible ? "表示" : "非表示";
                                        context.getSource().sendSuccess(() -> Component.literal("アーマーゲージを " + status + " に設定しました。"), true);
                                    });
                                    return 1;
                                }))
        );
    }
}