package com.mimic.monstermod.command;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.init.IdentityType;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class MonsterCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
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
                                                    IdentityType.getAllIds()
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


    private static int transformPlayer(CommandSourceStack source,
                                       ServerPlayer targetPlayer,
                                       boolean transform,
                                       ResourceLocation identityId) {

        targetPlayer.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(transformation -> {

                    if (transform) {
                        if (identityId == null) {
                            source.sendFailure(Component.literal("変身するにはIdentity IDを指定してください。"));
                            return;
                        }

                        if (!IdentityType.exists(identityId)) {
                            source.sendFailure(Component.literal("不明なIdentity ID: " + identityId));
                            return;
                        }

                        transformation.startTransformation(targetPlayer, identityId);

                        source.sendSuccess(() ->
                                        Component.literal(targetPlayer.getName().getString()
                                                + " を " + identityId.getPath() + " に変身させました。"),
                                true
                        );

                    } else {
                        transformation.stopTransformation(targetPlayer);

                        source.sendSuccess(() ->
                                        Component.literal(targetPlayer.getName().getString()
                                                + " の変身を解除しました。"),
                                true
                        );
                    }

                    transformation.syncToClient(targetPlayer);
                });

        return 1;
    }
}
