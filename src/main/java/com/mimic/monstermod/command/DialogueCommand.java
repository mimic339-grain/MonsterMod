package com.mimic.monstermod.command;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.dialogue.DialoguePage;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.DialogueStore;
import com.mimic.monstermod.dialogue.PortraitSpec;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_StartDialoguePacket;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/**
 * 会話(天の声)用コマンド。
 *
 *  /dialogue play <id> <targets>   指定プレイヤー/チームへ会話を再生する
 *  /dialogue list                  登録済みの会話IDを一覧表示
 *  /dialogue delete <id>           会話を削除
 *  /dialogue demo                  レイアウト確認用のサンプル会話を自分に再生
 *
 * <targets> はバニラのセレクタなので @a / @p / @e[team=xxx] などがそのまま使える
 * (チーム単位の配信もこれで実現できる)。
 * 実際の表示は S2C_StartDialoguePacket → DialogueScreen。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class DialogueCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dialogue")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("play")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> play(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        EntityArgument.getPlayers(ctx, "targets"))))))
                        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> delete(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("demo").executes(ctx -> demo(ctx.getSource())))
        );
    }

    // 保存済みの会話を対象プレイヤーへ送る(天の声)
    private static int play(CommandSourceStack src, String id, Collection<ServerPlayer> targets) {
        DialogueStore store = DialogueStore.get(src.getServer());
        DialogueSet set = store.getDialogue(id);
        if (set == null || set.isEmpty()) {
            src.sendFailure(Component.literal("会話が見つかりません: " + id));
            return 0;
        }
        for (ServerPlayer p : targets) {
            ModMessages.sendToPlayer(new S2C_StartDialoguePacket(set), p);
        }
        src.sendSuccess(() -> Component.literal("会話 '" + id + "' を " + targets.size() + "人へ再生しました"), true);
        return targets.size();
    }

    private static int list(CommandSourceStack src) {
        DialogueStore store = DialogueStore.get(src.getServer());
        var ids = store.listIds();
        if (ids.isEmpty()) {
            src.sendSuccess(() -> Component.literal("登録済みの会話はありません"), false);
        } else {
            src.sendSuccess(() -> Component.literal("会話一覧: " + String.join(", ", ids)), false);
        }
        return ids.size();
    }

    private static int delete(CommandSourceStack src, String id) {
        DialogueStore store = DialogueStore.get(src.getServer());
        if (store.remove(id)) {
            src.sendSuccess(() -> Component.literal("削除しました: " + id), true);
            return 1;
        }
        src.sendFailure(Component.literal("会話が見つかりません: " + id));
        return 0;
    }

    // レイアウト確認用。立ち絵なし/エンティティ立ち絵/震え文字を1本で確認できる
    private static int demo(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        DialogueSet set = new DialogueSet("__demo__");
        set.addPage(DialoguePage.simple("???",
                "…ようやく来たか。\n待ちわびたぞ、人の子よ。"));
        set.addPage(new DialoguePage("八咫烏",
                "我が名は八咫烏。この地を統べる者なり。\nここから先へ進みたくば、我を退けてみせよ。",
                PortraitSpec.entity(new ResourceLocation("monstermod", "yatagarasu")),
                "", DialoguePage.TextStyle.NORMAL, 0));
        set.addPage(new DialoguePage("八咫烏",
                "……ならば、死ぬがよい。",
                PortraitSpec.entity(new ResourceLocation("monstermod", "yatagarasu")),
                "", DialoguePage.TextStyle.SHAKE, 30));
        set.addPage(DialoguePage.simple("",
                "(立ち絵と名前が無い場合。テキストが左まで詰まって表示される。)"));
        ModMessages.sendToPlayer(new S2C_StartDialoguePacket(set), p);
        return 1;
    }
}
