package com.mimic.monstermod.network.client;

import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.DialogueStore;
import com.mimic.monstermod.item.DialogueEditorItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー。編集画面(DialogueEditorScreen)で作った会話を保存する。
 * サーバー側で DialogueStore(ワールドデータ)へ書き込むため、ワールドに永続化される。
 */
public class C2S_SaveDialoguePacket {

    private final DialogueSet set;

    public C2S_SaveDialoguePacket(DialogueSet set) {
        this.set = set;
    }

    public static void encode(C2S_SaveDialoguePacket msg, FriendlyByteBuf buf) {
        msg.set.write(buf);
    }

    public static C2S_SaveDialoguePacket decode(FriendlyByteBuf buf) {
        return new C2S_SaveDialoguePacket(DialogueSet.read(buf));
    }

    public static void handle(C2S_SaveDialoguePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null) return;
            if (msg.set == null || msg.set.getId().isEmpty()) return;

            // 編集はワールドを書き換えるためOP相当のみに限定する
            if (!player.hasPermissions(2)) {
                player.displayClientMessage(Component.literal("会話の編集には権限が必要です")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }

            DialogueStore.get(player.getServer()).put(msg.set);

            // 保存したIDを、手に持っている設定アイテムへ記録しておく。
            // これによりそのままエンティティを右クリックすれば紐付けできる
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof DialogueEditorItem) {
                    DialogueEditorItem.setDialogueId(held, msg.set.getId());
                    break;
                }
            }
            player.displayClientMessage(Component.literal(
                    "会話 '" + msg.set.getId() + "' を保存しました(" + msg.set.getPages().size() + "ページ)")
                    .withStyle(ChatFormatting.GREEN), false);
        });
        context.setPacketHandled(true);
    }
}
