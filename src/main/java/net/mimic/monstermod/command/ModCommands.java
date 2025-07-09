package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType; // StringArgumentTypeをインポート
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("monstermod")
                .requires(source -> source.hasPermission(2)) // 権限レベル2以上（OP相当）で実行可能
                .then(Commands.literal("transform")
                        .then(Commands.argument("target", EntityArgument.player()) // 変身させるプレイヤーを指定
                                .then(Commands.argument("monster_or_state", StringArgumentType.string()) // モンスター名または"false"を受け取る
                                        .executes(context -> transformPlayer(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "monster_or_state")
                                        ))
                                )
                        )
                )
        );
    }

    private static int transformPlayer(CommandSourceStack source, ServerPlayer player, String monsterOrState) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            // "false"が入力されたらプレイヤーに戻る
            if (monsterOrState.equalsIgnoreCase("false")) {
                if (transformation.isTransformed()) {
                    transformation.setTransformed(false);
                    transformation.setTransformedMobId(null);
                    player.sendSystemMessage(Component.literal("You transformed back to Steve!"));
                    ModMessages.sendToPlayer(new S2CTransformSyncPacket(false, null), player);
                } else {
                    player.sendSystemMessage(Component.literal("Already in player form!"));
                }
            }
            // "mimic"が入力されたらミミックに変身
            else if (monsterOrState.equalsIgnoreCase("mimic")) {
                // 将来的に他のモンスターを追加する場合はここを拡張
                ResourceLocation targetMobId = new ResourceLocation(MonsterMod.MOD_ID, "mimic");

                if (!transformation.isTransformed() || !targetMobId.equals(transformation.getTransformedMobId())) {
                    transformation.setTransformed(true);
                    transformation.setTransformedMobId(targetMobId);
                    player.sendSystemMessage(Component.literal("You transformed into a Mimic!"));
                    ModMessages.sendToPlayer(new S2CTransformSyncPacket(true, targetMobId), player);
                } else {
                    player.sendSystemMessage(Component.literal("Already transformed into a Mimic!"));
                }
            } else {
                // 未知のモンスター名が指定された場合
                source.sendFailure(Component.literal("Unknown monster name: " + monsterOrState + ". Use 'mimic' or 'false'."));
            }
        });

        return 1; // コマンド成功を示す値
    }
}