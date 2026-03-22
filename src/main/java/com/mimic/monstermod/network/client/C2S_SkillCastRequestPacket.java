package com.mimic.monstermod.network.client;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SpawnSkillLeadPacket;
import com.mimic.monstermod.skill.*;
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

            player.getCapability(com.mimic.monstermod.variable.CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                var identity = trans.getIdentity();
                if (identity == null) return;

                // 1. インデックス特定とCD判定（見つかった場合のみ厳密にチェック）
                int skillIndex = identity.findSkillIndex(msg.skillId);
                if (skillIndex != -1) {
                    int currentCD = identity.getCooldown(skillIndex);
                    if (currentCD > COOLDOWN_BUFFER) {
                        System.out.println("[Packet] Rejected: Still in Cooldown (" + currentCD + " ticks left)");
                        return;
                    }
                    // サーバー側の Identity 状態を更新
                    identity.handleAbility(player, skillIndex);
                }

                // 2. スキルデータの取得（Registryから直接取得することで Index 依存を回避）
                SkillLead lead = SkillLeadRegistry.getNullable(msg.skillId);
                if (lead == null) {
                    System.out.println("[Packet] Rejected: Unknown Skill ID " + msg.skillId);
                    return;
                }

                MathMain math = SkillLeadUtil.buildMath(lead, player.position());

                // 3. 他プレイヤーへのプレビュー同期
                ModMessages.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new S2C_SpawnSkillLeadPacket(player.getId(), lead, math)
                );

                // 4. サーバー側のダメージ・停止ロジック (SkillUtil) へ流す
                SkillUtil.execute(player.serverLevel(), player, lead, math);

                System.out.println("[Packet] Successfully executed: " + msg.skillId);
            });
        });
        context.setPacketHandled(true);
    }
}