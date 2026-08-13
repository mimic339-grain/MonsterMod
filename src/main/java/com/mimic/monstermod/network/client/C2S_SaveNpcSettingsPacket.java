package com.mimic.monstermod.network.client;

import com.mimic.monstermod.item.NpcToolItem;
import com.mimic.monstermod.npc.NpcSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー。NPC設定画面の内容を、手に持っているNPCツールへ記録する。
 * 以後そのツールでブロック/エンティティを右クリックすると、この設定で設置・NPC化される。
 */
public class C2S_SaveNpcSettingsPacket {

    private final String typeId;
    private final boolean immobile, noGravity, invulnerable, fireProof;
    private final NpcSettings.LookMode lookMode;

    public C2S_SaveNpcSettingsPacket(String typeId, boolean immobile, boolean noGravity,
                                     boolean invulnerable, boolean fireProof,
                                     NpcSettings.LookMode lookMode) {
        this.typeId = typeId;
        this.immobile = immobile;
        this.noGravity = noGravity;
        this.invulnerable = invulnerable;
        this.fireProof = fireProof;
        this.lookMode = lookMode;
    }

    public static void encode(C2S_SaveNpcSettingsPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.typeId);
        buf.writeBoolean(m.immobile);
        buf.writeBoolean(m.noGravity);
        buf.writeBoolean(m.invulnerable);
        buf.writeBoolean(m.fireProof);
        buf.writeEnum(m.lookMode);
    }

    public static C2S_SaveNpcSettingsPacket decode(FriendlyByteBuf buf) {
        return new C2S_SaveNpcSettingsPacket(buf.readUtf(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readEnum(NpcSettings.LookMode.class));
    }

    public static void handle(C2S_SaveNpcSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof NpcToolItem) {
                    NpcToolItem.setEntityTypeId(held, msg.typeId);
                    NpcToolItem.setSettings(held, new NpcSettings(
                            msg.immobile, msg.noGravity, msg.invulnerable, msg.fireProof,
                            msg.lookMode, 0f, 0, 0, 0));
                    player.displayClientMessage(Component.literal("NPC設定を保存しました: " + msg.typeId)
                            .withStyle(ChatFormatting.GREEN), true);
                    break;
                }
            }
        });
        context.setPacketHandled(true);
    }
}
