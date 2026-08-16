package com.mimic.monstermod.command;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.bomb.BombAttachment;
import com.mimic.monstermod.bomb.BombStore;
import com.mimic.monstermod.bomb.BombTiming;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/**
 * ボム関連のコマンド。
 *
 *  /bomb timeset <秒>   … タイマーの長さを固定する(遊ぶテンポを決めるため)
 *  /bomb timerandom     … 既定の1〜5分ランダムに戻す
 *  /bomb clear <対象>   … 対象に付いたボムを強制的に外す(進行不能になったとき用)
 *  /bomb list           … 今ワールドに仕掛けられているブロックボムの数を見る
 *
 * 時間の設定はワールド単位で保存されるので、入り直しても保たれる。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class BombCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bomb")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("timeset")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(ctx -> {
                                    int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    BombStore.get(level).setFixedFuse(sec * BombTiming.TICKS_PER_SECOND);

                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "ボムのタイマーを " + sec + "秒 に固定しました"), true);
                                    return 1;
                                })))

                .then(Commands.literal("timerandom")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            BombStore.get(level).setFixedFuse(0);

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "ボムのタイマーを 1〜5分のランダム に戻しました"), true);
                            return 1;
                        }))

                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> {
                                    Collection<? extends Entity> targets =
                                            EntityArgument.getEntities(ctx, "targets");
                                    int total = 0;
                                    for (Entity e : targets) total += BombAttachment.clear(e);

                                    final int removed = total;
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "ボムを " + removed + " 個外しました"), true);
                                    return removed;
                                })))

                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            BombStore store = BombStore.get(level);
                            int count = store.all().size();
                            int fixed = store.getFixedFuse();

                            String timing = fixed > 0
                                    ? (fixed / BombTiming.TICKS_PER_SECOND) + "秒 固定"
                                    : "1〜5分 ランダム";

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "仕掛け中のブロックボム: " + count + " 個 / タイマー設定: " + timing), false);
                            return count;
                        }))
        );
    }
}
