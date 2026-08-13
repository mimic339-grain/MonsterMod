package com.mimic.monstermod.network.server;

import com.mimic.monstermod.client.NpcEditorScreen;
import com.mimic.monstermod.npc.NpcSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** サーバー → クライアント。NPC作成ツールの設定画面を、現在の設定入りで開く */
public class S2C_OpenNpcEditorPacket {

    private final String typeId;
    private final NpcSettings settings;

    public S2C_OpenNpcEditorPacket(String typeId, NpcSettings settings) {
        this.typeId = typeId == null ? "" : typeId;
        this.settings = settings;
    }

    public static void encode(S2C_OpenNpcEditorPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.typeId);
        msg.settings.write(buf);
    }

    public static S2C_OpenNpcEditorPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        return new S2C_OpenNpcEditorPacket(id, NpcSettings.read(buf));
    }

    public static void handle(S2C_OpenNpcEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // クライアント専用クラスに触れるためDistExecutorで隔離する
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                Minecraft.getInstance().setScreen(new NpcEditorScreen(msg.typeId, msg.settings))));
        context.setPacketHandled(true);
    }
}
