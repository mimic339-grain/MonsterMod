package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.DialogueScreen;
import com.mimic.monstermod.dialogue.DialogueSet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント。会話の再生開始を通知する。
 *
 * 会話の定義そのものを丸ごと送るため、クライアント側に定義を事前配布しておく必要がなく、
 * ゲーム内で作った会話をその場で全員に見せられる(同期漏れが起きない)。
 * 送信元は DialogueCommand(天の声) や エンティティ右クリック。
 */
public class S2C_StartDialoguePacket {

    private final DialogueSet set;

    public S2C_StartDialoguePacket(DialogueSet set) {
        this.set = set;
    }

    public static void encode(S2C_StartDialoguePacket msg, FriendlyByteBuf buf) {
        msg.set.write(buf);
    }

    public static S2C_StartDialoguePacket decode(FriendlyByteBuf buf) {
        return new S2C_StartDialoguePacket(DialogueSet.read(buf));
    }

    public static void handle(S2C_StartDialoguePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                // クライアント専用クラスに触れるため、サーバー側でロードされないようDistExecutorで隔離する
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openScreen(msg))
        );
        context.setPacketHandled(true);
    }

    private static void openScreen(S2C_StartDialoguePacket msg) {
        if (msg.set == null || msg.set.isEmpty()) return;
        Minecraft.getInstance().setScreen(new DialogueScreen(msg.set));
    }
}
