package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.DialogueEditorScreen;
import com.mimic.monstermod.dialogue.DialogueSet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント。会話の編集画面を開く。
 *
 * 会話の実体はサーバー(ワールドデータ)にあるため、
 * 既存の会話を編集できるよう「現在の内容」を一緒に送る。
 * 新規作成の場合は空のセットが入る。
 */
public class S2C_OpenDialogueEditorPacket {

    private final String id;
    private final DialogueSet existing; // 空なら新規

    public S2C_OpenDialogueEditorPacket(String id, DialogueSet existing) {
        this.id = id == null ? "" : id;
        this.existing = existing == null ? new DialogueSet("") : existing;
    }

    public static void encode(S2C_OpenDialogueEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.id);
        msg.existing.write(buf);
    }

    public static S2C_OpenDialogueEditorPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        DialogueSet set = DialogueSet.read(buf);
        return new S2C_OpenDialogueEditorPacket(id, set);
    }

    public static void handle(S2C_OpenDialogueEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                // クライアント専用クラスに触れるためDistExecutorで隔離する
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openScreen(msg))
        );
        context.setPacketHandled(true);
    }

    private static void openScreen(S2C_OpenDialogueEditorPacket msg) {
        Minecraft.getInstance().setScreen(
                new DialogueEditorScreen(msg.id, msg.existing.isEmpty() ? null : msg.existing));
    }
}
