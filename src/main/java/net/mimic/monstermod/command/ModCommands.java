package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.PlayerTransformC2SPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 変身コマンド
        // LiteralCommandNodeのインポートは不要なため削除しました。
        dispatcher.register(Commands.literal("transform")
                .requires(source -> source.hasPermission(2)) // オペレーター権限レベル2以上
                .then(Commands.argument("target", EntityArgument.player()) // ターゲットプレイヤー
                        .then(Commands.argument("transform", BoolArgumentType.bool()) // 変身するかどうか (true/false)
                                .executes(context -> transformPlayer(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        BoolArgumentType.getBool(context, "transform"),
                                        null // 変身解除時はID不要
                                ))
                                .then(Commands.argument("identityId", StringArgumentType.string()) // 変身先のIdentity ID (Stringとして受け取る)
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResourceLocation(
                                                PlayerIdentityRegistry.getAllIdentityIds().stream(), builder)) // SuggestionProviderで全てのIdentity IDを提示
                                        .executes(context -> transformPlayer(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                BoolArgumentType.getBool(context, "transform"),
                                                new ResourceLocation(StringArgumentType.getString(context, "identityId")) // StringからResourceLocationに変換
                                        ))
                                )
                        )
                ).build() // ★ ここに .build() を追加しました
        );
    }

    private static int transformPlayer(CommandSourceStack source, ServerPlayer targetPlayer, boolean transform, ResourceLocation identityId) throws CommandSyntaxException {
        // サーバーサイドで実行される
        targetPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transform) {
                if (identityId == null) {
                    source.sendFailure(Component.literal("変身するにはIdentity IDを指定してください。"));
                    return;
                }
                if (!PlayerIdentityRegistry.hasIdentity(identityId)) {
                    source.sendFailure(Component.literal("不明なIdentity ID: " + identityId));
                    return;
                }
                transformation.setTransformedMobId(identityId);
                transformation.setTransformed(true);
                // Mimicに変身する場合、初期状態をIDLEに設定
                if (identityId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {
                    transformation.setMimicState(net.mimic.monstermod.entity.custom.MimicEntity.MimicAnimationState.IDLE);
                    transformation.setBiting(false);
                }
                source.sendSuccess(() -> Component.literal(targetPlayer.getName().getString() + " を " + identityId.getPath() + " に変身させました。"), true);
            } else {
                transformation.setTransformed(false);
                transformation.setTransformedMobId(null);
                // 変身解除時、Mimic関連の状態をリセット
                transformation.setMimicState(net.mimic.monstermod.entity.custom.MimicEntity.MimicAnimationState.IDLE);
                transformation.setBiting(false);
                source.sendSuccess(() -> Component.literal(targetPlayer.getName().getString() + " の変身を解除しました。"), true);
            }
            transformation.syncToClient(targetPlayer);
        });
        return 1;
    }
}