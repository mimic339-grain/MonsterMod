package com.mimic.monstermod.network.server;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバーがC2S_SkillCastRequestPacketを拒否した(発動できなかった)ことを
 * 要求元クライアントに通知するパケット。
 *
 * 【背景】
 * BaseIdentity/HunterIdentity.handleAbility() はサーバーの応答を待たずに
 * クライアント側で仮に lockCooldowns[skillIndex] をセットしてから
 * C2S_SkillCastRequestPacket を送信する。サーバー側のSkillUtil.tryExecute()等が
 * 発動を拒否した場合、従来は何も返信されず、クライアントの仮ロックが
 * 自然減衰(約1秒)するまで再入力を受け付けられなかった(連打時に発動できない不具合の原因)。
 * このパケットでロックを即座に解除する。
 */
public class S2C_SkillCastRejectedPacket {
    private final SkillId skillId;

    public S2C_SkillCastRejectedPacket(SkillId skillId) {
        this.skillId = skillId;
    }

    public static void encode(S2C_SkillCastRejectedPacket msg, FriendlyByteBuf buf) {
        msg.skillId.write(buf);
    }

    public static S2C_SkillCastRejectedPacket decode(FriendlyByteBuf buf) {
        return new S2C_SkillCastRejectedPacket(SkillId.read(buf));
    }

    public static void handle(S2C_SkillCastRejectedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // ハンターが優先(processSkillCastの優先順位と合わせる)、なければ通常変身側
            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(hunter -> {
                if (hunter.isActive() && hunter.getIdentity() != null
                        && hunter.getIdentity().findSkillIndex(msg.skillId) != -1) {
                    hunter.getIdentity().clearLock(hunter.getIdentity().findSkillIndex(msg.skillId));
                    return;
                }
                player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                    if (trans.getIdentity() != null) {
                        int idx = trans.getIdentity().findSkillIndex(msg.skillId);
                        if (idx != -1) trans.getIdentity().clearLock(idx);
                    }
                });
            });
        });
        context.setPacketHandled(true);
    }
}
