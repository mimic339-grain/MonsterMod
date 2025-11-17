package com.mimic.monstermod.command;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.atomic.AtomicInteger;

public class ResetIdentityHPCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("resetIdentityHP")
                        .requires(source -> source.hasPermission(2)) // OP 権限

                        // /resetIdentityHP <targets>
                        // @a / @p / @s / 名前 TAB 補完全部 OK
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {

                                    AtomicInteger count = new AtomicInteger();

                                    for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
                                        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                                                .ifPresent(cap -> MonsterTransformUtil.resetAllIdentityHPs(player));
                                        count.incrementAndGet();
                                    }

                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    count.get() + "人のプレイヤーの IdentityHP をリセットしました。"
                                            ),
                                            true
                                    );

                                    return count.get();
                                })
                        )
        );
    }
}
