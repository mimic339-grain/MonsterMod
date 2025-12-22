package com.mimic.monstermod.network.server;

import com.mimic.monstermod.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー→クライアント用アニメーション再生パケット
 */
public class S2CPlayAnimationPacket {

    private final int targetEntityId;
    private final String animationId;
    private final boolean override; // 既存アニメーションを上書きするか

    public S2CPlayAnimationPacket(int targetEntityId, String animationId, boolean override) {
        this.targetEntityId = targetEntityId;
        this.animationId = animationId;
        this.override = override;
    }

    // バッファに書き込み
    public static void encode(S2CPlayAnimationPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.targetEntityId);
        buf.writeUtf(pkt.animationId);
        buf.writeBoolean(pkt.override);
    }

    // バッファから読み込み
    public static S2CPlayAnimationPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        String anim = buf.readUtf();
        boolean override = buf.readBoolean();
        return new S2CPlayAnimationPacket(id, anim, override);
    }

    // 受信時処理（クライアント）
    public static void handle(S2CPlayAnimationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            var entity = mc.level.getEntity(pkt.targetEntityId);
            if (!(entity instanceof net.minecraft.client.player.AbstractClientPlayer player)) return;

            // ModifierLayer を取得
            var layer = (dev.kosmx.playerAnim.api.layered.ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation>)
                    dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAssociatedData(player)
                            .get(new ResourceLocation("monstermod", "player_animation"));

            if (layer != null && (pkt.override || !layer.isActive())) {
                var anim = dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry
                        .getAnimation(new ResourceLocation("monstermod", pkt.animationId));
                if (anim != null) {
                    layer.setAnimation(new dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer(anim));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // サーバー側ユーティリティ: 全クライアントに送信
    public static void sendToAll(Player target, String animationId, boolean override) {
        if (!(target.level().isClientSide)) {
            var packet = new S2CPlayAnimationPacket(target.getId(), animationId, override);
            for (Player p : target.level().players()) {
                if (p instanceof ServerPlayer sp) {
                    ModMessages.sendToPlayer(packet, sp);
                }
            }
        }
    }

    // サーバー側ユーティリティ: 特定クライアントに送信
    public static void sendToClient(Player target, ServerPlayer sp, String animationId, boolean override) {
        if (!(target.level().isClientSide)) {
            var packet = new S2CPlayAnimationPacket(target.getId(), animationId, override);
            ModMessages.sendToPlayer(packet, sp);
        }
    }
}