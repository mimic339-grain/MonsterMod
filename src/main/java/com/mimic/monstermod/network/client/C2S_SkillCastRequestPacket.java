package com.mimic.monstermod.network.client;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SpawnSkillLeadPacket;
import com.mimic.monstermod.skill.*;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class C2S_SkillCastRequestPacket {
    private final SkillId skillId;

    public C2S_SkillCastRequestPacket(SkillId skillId) {
        this.skillId = skillId;
    }

    public static void encode(C2S_SkillCastRequestPacket msg, FriendlyByteBuf buf) {
        msg.skillId.write(buf);
    }

    public static C2S_SkillCastRequestPacket decode(FriendlyByteBuf buf) {
        return new C2S_SkillCastRequestPacket(SkillId.read(buf));
    }

    public static void handle(C2S_SkillCastRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                var identity = trans.getIdentity();
                if (identity == null) return;

                int skillIndex = identity.findSkillIndex(msg.skillId);
                if (skillIndex != -1) {
                    // クールダウン中、または予兆ロック中なら無視
                    if (identity.getCooldown(skillIndex) > 0 || identity.isLocking(skillIndex)) return;
                }

                SkillLead lead = SkillLeadRegistry.getNullable(msg.skillId);
                if (lead == null) return;

                MathMain math = SkillLeadUtil.buildMath(lead, player.position());

                if (SkillUtil.tryExecute(player.serverLevel(), player, lead, math)) {
                    ModMessages.INSTANCE.send(
                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new S2C_SpawnSkillLeadPacket(player.getId(), lead, math)
                    );

                    if (skillIndex != -1) {
                        identity.handleAbility(player, skillIndex);
                    }
                }
            });
        });
        context.setPacketHandled(true);
    }
}