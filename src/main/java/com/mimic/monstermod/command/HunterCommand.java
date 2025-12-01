package com.mimic.monstermod.command;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class HunterCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("hunter")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("enable", BoolArgumentType.bool())
                                        .executes(context -> {
                                            Collection<ServerPlayer> targets =
                                                    EntityArgument.getPlayers(context, "target");

                                            boolean enable = BoolArgumentType.getBool(context, "enable");

                                            return toggleHunter(context.getSource(), targets, enable);
                                        }))))
        ;
    }

    private static int toggleHunter(CommandSourceStack source,
                                    Collection<ServerPlayer> targets, boolean enable) {
        for (ServerPlayer targetPlayer : targets) {
            targetPlayer.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .ifPresent(hunter -> {

                        if (enable) {
                            hunter.startHunter(targetPlayer);
                            source.sendSuccess(() ->
                                    Component.literal(targetPlayer.getName().getString() + " を Hunter にしました。"), true);
                        } else {
                            hunter.stopHunter(targetPlayer);
                            source.sendSuccess(() ->
                                    Component.literal(targetPlayer.getName().getString() + " の Hunter を解除しました。"), true);
                        }

                        hunter.syncToClient(targetPlayer);
                    });
        }
        return targets.size();
    }
}
