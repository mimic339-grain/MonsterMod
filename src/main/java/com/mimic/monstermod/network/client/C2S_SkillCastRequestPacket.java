package com.mimic.monstermod.network.client;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SpawnSkillLeadPacket;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.SkillLeadUtil;
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
    public static void processSkillCast(ServerPlayer player, com.mimic.monstermod.identity.BaseIdentity identity, SkillId skillId) {
        // サーバー側の Identity インデックス確認
        // 【デバッグ用】実行前に Identity の中身をチェック
        MonsterMod.LOGGER.info("DEBUG: Casting Skill: {}, Identity Instance: {}, SkillArray: {}",
                skillId, System.identityHashCode(identity), java.util.Arrays.toString(identity.getSkillIds()));

        int skillIndex = identity.findSkillIndex(skillId);
        if (skillIndex == -1) return;

        // サーバー側でのクールダウン/ロック最終チェック
        if (identity.getCooldown(skillIndex) > 0 || identity.isLocking(skillIndex)) return;

        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        MathMain math = SkillLeadUtil.buildMath(lead, player.position());

        // --- 実行判定と状態適用 ---
        // 1. SkillUtil で「スケジュールとして実行可能か」を判定し登録
        if (com.mimic.monstermod.skill.SkillUtil.tryExecute(player.serverLevel(), player, lead, math)) {

            // 2. 成功した場合のみ、サーバー側の Identity のCDとLockを確定させる
            // ※ handleAbility を呼ぶとまたパケット送信処理に入ってしまうため、applyCastResult を直接呼ぶ
            identity.applyCastResult(lead);

            // 3. 自分を含む周囲のプレイヤーに「描画していいよ」とパケットを送信
            ModMessages.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new S2C_SpawnSkillLeadPacket(player.getId(), lead, math)
            );
        }

    }
}