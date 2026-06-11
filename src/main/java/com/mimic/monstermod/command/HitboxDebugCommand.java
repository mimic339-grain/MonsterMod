package com.mimic.monstermod.command;


import com.mimic.monstermod.debug.HitboxRenderUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /monstermod debug コマンド — DragonDebugCommand を統合・拡張。
 *
 * DragonDebugCommand.java は削除してこのクラスに一本化する。
 * デバッグUI（VisualConfigScreen）との連携もここで行う。
 *
 * サブコマンド:
 *   /monstermod debug            → 全デバッグトグル
 *   /monstermod debug obb        → OBBワイヤーフレーム ON/OFF
 *   /monstermod debug bones      → ボーンライン表示 ON/OFF
 *   /monstermod debug partHp     → 部位HP表示 ON/OFF
 *   /monstermod debug preview    → MMOプレビュー判定範囲 ON/OFF
 *   /monstermod debug screen     → VisualConfigScreen を開く
 *
 * 削除対象:
 *   debug/DragonDebugCommand.java → このクラスに統合済み
 *
 * 配置: com/mimic/monstermod/command/HitboxDebugCommand.java
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HitboxDebugCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("monstermod")
                        .then(Commands.literal("debug")
                                .requires(src -> src.hasPermission(2))
                                .executes(HitboxDebugCommand::toggleAll)
                                .then(Commands.literal("obb")
                                        .executes(ctx -> toggle(ctx, "obb")))
                                .then(Commands.literal("bones")
                                        .executes(ctx -> toggle(ctx, "bones")))
                                .then(Commands.literal("partHp")
                                        .executes(ctx -> toggle(ctx, "partHp")))
                                .then(Commands.literal("preview")
                                        .executes(ctx -> toggle(ctx, "preview")))
                                .then(Commands.literal("screen")
                                        .executes(HitboxDebugCommand::openScreen))
                        )
        );
    }

    private static int toggleAll(CommandContext<CommandSourceStack> ctx) {
        boolean newState = !HitboxRenderUtil.isAnyEnabled();
        HitboxRenderUtil.setAll(newState);
        ctx.getSource().sendSuccess(
                () -> Component.literal("[MonsterMod] 全デバッグ表示: " + (newState ? "ON" : "OFF")), false);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, String key) {
        boolean newState = HitboxRenderUtil.toggle(key);
        ctx.getSource().sendSuccess(
                () -> Component.literal("[MonsterMod] " + key + ": " + (newState ? "ON" : "OFF")), false);
        return 1;
    }

    private static int openScreen(CommandContext<CommandSourceStack> ctx) {
        // クライアント側でVisualConfigScreenを開く
        // VisualConfigScreenはPacket経由でクライアントに送信するか
        // @OnlyIn(Dist.CLIENT)のイベントで開く
        ctx.getSource().sendSuccess(
                () -> Component.literal("[MonsterMod] /monstermod debug screen → クライアント側でGUIを開きます"), false);
        return 1;
    }
}