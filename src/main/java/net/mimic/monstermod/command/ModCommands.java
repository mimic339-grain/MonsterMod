package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
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
                //定義
                Commands.literal("transform")
                        //権限
                        .requires(source -> source.hasPermission(2))
                        //プレイヤー指定
                        .then(Commands.argument("target", EntityArgument.player())
                                //true false処理
                                .then(Commands.argument("transform", BoolArgumentType.bool())
                                        //変身処理
                                        .executes(context -> transformPlayer(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                BoolArgumentType.getBool(context, "transform"),
                                                //identityId が指定されていない場合は null
                                                null
                                        ))
                                        .then(Commands.argument("identityId", ResourceLocationArgument.id())
                                                //タブ補完
                                                .suggests((context, builder) -> {
                                                    BaseMonsterIdentityRegistry.getAllIdentityIds()
                                                            .forEach(id -> builder.suggest(id.toString()));
                                                    return builder.buildFuture();
                                                })
                                                //IDで変身処理
                                                .executes(context -> transformPlayer(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        BoolArgumentType.getBool(context, "transform"),
                                                        ResourceLocationArgument.getId(context, "identityId") // ← ここも変更
                                                ))
                                        )
                                )
                        )
        );
    }
    //処理メソッド
    private static int transformPlayer(CommandSourceStack source, ServerPlayer targetPlayer, boolean transform, ResourceLocation identityId) throws CommandSyntaxException {
        //変身状態の取得
        targetPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transform) {
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

                source.sendSuccess(() -> Component.literal(
                        targetPlayer.getName().getString() + " を " + identityId.getPath() + " に変身させました。"), true);

            } else {
                // 変身解除時は通常のプレイヤーに戻る
                transformation.setTransformed(false);
                transformation.setTransformedMobId(null);

                source.sendSuccess(() -> Component.literal(
                        targetPlayer.getName().getString() + " の変身を解除しました。"), true);
            }

            // 変身状態をクライアントに同期
            transformation.syncToClient(targetPlayer);
        });
        //０失敗:１成功
        return 1;
    }
}
