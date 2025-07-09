package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

import java.util.stream.Collectors;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("transform")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("mob_id", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                                                .map(ResourceLocation::toString)
                                                .filter(id -> id.startsWith("minecraft:") || id.startsWith(MonsterMod.MOD_ID + ":"))
                                                .collect(Collectors.toList()), builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String mobIdString = StringArgumentType.getString(context, "mob_id");
                                    ResourceLocation targetMobId = new ResourceLocation(mobIdString);

                                    player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                                        transformation.setTransformed(true);
                                        transformation.setTransformedMobId(targetMobId);
                                        transformation.setMimicOpen(false);

                                        player.sendSystemMessage(Component.literal("Transformed into: " + targetMobId));

                                        ModMessages.sendToPlayer(new S2CTransformSyncPacket(true, targetMobId, false), player);
                                    });
                                    return 1;
                                })
                        ))
                .then(Commands.literal("revert")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                                transformation.setTransformed(false);
                                transformation.setTransformedMobId(null);
                                transformation.setMimicOpen(false);

                                player.sendSystemMessage(Component.literal("Reverted to human form."));

                                ModMessages.sendToPlayer(new S2CTransformSyncPacket(false, null, false), player);
                            });
                            return 1;
                        })
                )
        );
    }
}