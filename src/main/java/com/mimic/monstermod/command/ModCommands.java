package com.mimic.monstermod.command;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {

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
                                                    BaseMonsterIdentityRegistry.getAllIdentityIds()
                                                            .forEach(id -> builder.suggest(id.toString()));
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

    private static int transformPlayer(CommandSourceStack source, ServerPlayer targetPlayer, boolean transform, ResourceLocation identityId) throws CommandSyntaxException {
        targetPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {
                    if (transform) {
                        // 変身
                        if (identityId == null) {
                            source.sendFailure(Component.literal("変身するにはIdentity IDを指定してください。"));
                            return;
                        }
                        if (!BaseMonsterIdentityRegistry.hasIdentity(identityId)) {
                            source.sendFailure(Component.literal("不明なIdentity ID: " + identityId));
                            return;
                        }

                        transformation.setTransformedMobId(identityId);
                        transformation.setTransformed(true);

                        // 変身開始
                        transformation.startTransformation(targetPlayer, identityId);

                        source.sendSuccess(() -> Component.literal(
                                targetPlayer.getName().getString() + " を " + identityId.getPath() + " に変身させました。"), true);

                    } else {
                        // 変身解除
                        transformation.setTransformed(false);
                        transformation.setTransformedMobId(null);

                        transformation.stopTransformation(targetPlayer);

                        source.sendSuccess(() -> Component.literal(
                                targetPlayer.getName().getString() + " の変身を解除しました。"), true);
                    }

                    // クライアント同期
                    transformation.syncToClient(targetPlayer);
                });

        return 1;
    }
}
