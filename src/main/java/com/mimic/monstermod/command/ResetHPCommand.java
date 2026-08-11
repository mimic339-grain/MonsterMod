package com.mimic.monstermod.command;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class ResetHPCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("resethp")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {

                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");

                                    resetPlayers(players);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("HPをリセットしました（PlayerHP + IdentityHP）"), true
                                    );

                                    return 1;
                                })
                        )
        );
    }

    private static void resetPlayers(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {

            // PlayerHP / IdentityHP を最大値に戻す(Capability側に保持されるため、
            // Forgeの標準セーブ処理で自動的に永続化される)
            MonsterTransformUtil.resetPlayerHP(player);
            MonsterTransformUtil.resetIdentityHP(player);

            // クライアント同期
            CapabilityRegistry.syncToClient(player);
        }
    }
}
