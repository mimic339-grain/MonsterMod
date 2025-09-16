package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.mimic.monstermod.networking.PlayerTransformHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ModCommands {

    private static final List<String> MONSTER_IDS = List.of("monstermod:mimic");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("transform")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("transform", BoolArgumentType.bool())
                                        .executes(context -> transformPlayer(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                BoolArgumentType.getBool(context, "transform"),
                                                null
                                        ))
                                        .then(Commands.argument("identityId", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> {
                                                    MONSTER_IDS.forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> transformPlayer(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        BoolArgumentType.getBool(context, "transform"),
                                                        ResourceLocationArgument.getId(context, "identityId")
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int transformPlayer(CommandSourceStack source, ServerPlayer targetPlayer, boolean transform, ResourceLocation identityId) {
        PlayerTransformHandler.handleTransformRequest(targetPlayer, transform, identityId);
        return 1;
    }
}
