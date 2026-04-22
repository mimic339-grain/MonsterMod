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

            // 1. まずはハンターのCapabilityを確認
            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(hunter -> {
                if (hunter.isActive() && hunter.getIdentity() != null) {
                    // ハンターがアクティブなら、ハンターのIdentityで実行
                    processSkillCast(player, hunter.getIdentity(), msg.skillId);
                } else {
                    // ハンターでないなら、従来の変身Capabilityを確認
                    player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                        if (trans.getIdentity() != null) {
                            processSkillCast(player, trans.getIdentity(), msg.skillId);
                        }
                    });
                }
            });
        });
        context.setPacketHandled(true);
    }

    /**
     * 指定された Identity を使用してスキル実行を試みる内部メソッド
     * (元のコードのロジックを崩さないように切り出し)
     */
    private static void processSkillCast(ServerPlayer player, com.mimic.monstermod.identity.BaseIdentity identity, SkillId skillId) {
        int skillIndex = identity.findSkillIndex(skillId);

        // クールダウン・ロック中なら中断
        if (skillIndex != -1) {
            if (identity.getCooldown(skillIndex) > 0 || identity.isLocking(skillIndex)) return;
        }

        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        MathMain math = SkillLeadUtil.buildMath(lead, player.position());

        // 共通の実行ユーティリティ
        if (SkillUtil.tryExecute(player.serverLevel(), player, lead, math)) {
            // クライアント(周囲のプレイヤー含む)へスキル描画パケットを送信
            ModMessages.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new S2C_SpawnSkillLeadPacket(player.getId(), lead, math)
            );

            // Identity側の事後処理（ここでCDセットやCapabilityの同期が走る）
            if (skillIndex != -1) {
                identity.handleAbility(player, skillIndex);
            }
        }
    }
}