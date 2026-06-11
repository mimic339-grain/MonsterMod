package com.mimic.monstermod.network.packets;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: スキル発動リクエスト。
 *
 * EFM参考:
 *   - network/common/CPActionStatus.java — C2Sアクションリクエスト
 *
 * フロー:
 *   1. クライアントでキー入力を検知
 *   2. C2SSkillCastPacket をサーバーへ送信
 *   3. サーバーで権限チェック → BaseSkill.activate() 呼び出し
 *   4. S2CSkillStatePacket を全クライアントにブロードキャスト
 *
 * ※ スキル発動の最終判断はサーバー側で行う（不正防止）
 *
 * 配置: com/mimic/monstermod/network/packets/C2SSkillCastPacket.java
 */
public class C2SSkillCastPacket {

    private final int    slotIndex;  // 押されたスキルスロット番号
    private final String skillId;    // スキルID（冗長チェック用）
    private final boolean isCombo;   // コンボ入力か通常発動か

    public C2SSkillCastPacket(int slotIndex, String skillId, boolean isCombo) {
        this.slotIndex = slotIndex;
        this.skillId   = skillId;
        this.isCombo   = isCombo;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeUtf(skillId);
        buf.writeBoolean(isCombo);
    }

    public static C2SSkillCastPacket decode(FriendlyByteBuf buf) {
        return new C2SSkillCastPacket(buf.readInt(), buf.readUtf(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
                // ── サーバー権限チェック ─────────────────────────
                // 1. スロットに登録されているスキルIDと一致するか確認
                String registeredSkill = cap.getSkillSlot(slotIndex);
                if (registeredSkill == null || !registeredSkill.equals(skillId)) return;

                // 2. クールダウン中でないか確認
                if (cap.getCooldown(slotIndex) > 0) return;

                // 3. スキルを発
// --- C2SSkillCastPacket の handle 内 ---
                SkillLead lead = SkillLeadRegistry.getSkill(new SkillId(new ResourceLocation(skillId)));
                if (lead == null) return;

// 設計図(lead)からインスタンスを生成
                BaseSkill skill = lead.createInstance(player);
                if (skill == null) return; // 生成失敗なら終了

                if (isCombo) {
                    skill.reserveCombo(skillId);
                } else {
                    if (skill.activate(player)) {
                        cap.tryUseSkill(slotIndex, skill.getCooldown());
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}