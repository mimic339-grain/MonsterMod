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
    private static final int COOLDOWN_BUFFER = 5;

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

                // クールダウンチェック
                if (skillIndex != -1) {
                    if (identity.getCooldown(skillIndex) > COOLDOWN_BUFFER) return;
                }

                SkillLead lead = SkillLeadRegistry.getNullable(msg.skillId);
                if (lead == null) return;

                MathMain math = SkillLeadUtil.buildMath(lead, player.position());

                // サーバー側での実行承認
                if (SkillUtil.tryExecute(player.serverLevel(), player, lead, math)) {

                    // 周囲のプレイヤーに予兆を表示
                    ModMessages.INSTANCE.send(
                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new S2C_SpawnSkillLeadPacket(player.getId(), lead, math)
                    );

                    // ★ 重要: Identity側のhandleAbilityを「サーバー側」として実行
                    // これにより、Identity側で AttackSpec.apply() が呼ばれるようになります
                    if (skillIndex != -1) {
                        identity.handleAbility(player, skillIndex);
                    }
                }
            });
        });
        context.setPacketHandled(true);
    }
}