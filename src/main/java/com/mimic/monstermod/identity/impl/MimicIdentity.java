package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.identity.util.MimicSkillLeads;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SkillCastRequestPacket;
import com.mimic.monstermod.network.server.S2CMimicDodgePacket;
import com.mimic.monstermod.overlay.ClientEvents;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.SkillLeadUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MimicIdentity extends BaseMonsterIdentity {

    private static final SkillId[] SKILLS = {
            MimicSkillLeads.TEST_2D,
            MimicSkillLeads.TEST_BLOCK,
            MimicSkillLeads.TEST_3D,
            MimicSkillLeads.TEST_EMERGENCY, // インデックス 3
    };

    // 各スキルの基本クールダウン(ticks)
    private static final int[] COOLDOWNS = { 160, 100, 120, 70 };

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILLS.length);
    }

    @Override
    public void handleAbility(Player player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= SKILLS.length) return;

        SkillId skillId = SKILLS[skillIndex];

        // ★ 緊急回避スキルの場合、専用の回避メソッドへ飛ばす
        if (skillId.equals(MimicSkillLeads.TEST_EMERGENCY)) {
            handleDodge(player);
            return;
        }

        // 通常スキルの処理
        int currentCD = getCooldown(skillIndex);
        if (player.level().isClientSide()) {
            if (currentCD > 0) return;

            SkillLead lead = SkillLeadRegistry.getNullable(skillId);
            if (lead == null) return;

            // クライアント側で即座にCD表示を開始（予兆時間 + 本CD）
            this.abilityCooldowns[skillIndex] = lead.totalPreviewTicks + COOLDOWNS[skillIndex];

            System.out.println("[クライアント] スキル予約: " + skillId);
            MathMain math = SkillLeadUtil.buildMath(lead, player.position());
            ClientEvents.spawnLocal(player, lead, math);

            ModMessages.INSTANCE.sendToServer(new C2S_SkillCastRequestPacket(skillId));

        } else {
            // サーバー側：二重発動防止のロック
            if (this.abilityCooldowns[skillIndex] <= 0) {
                this.abilityCooldowns[skillIndex] = 1;
                System.out.println("[サーバー] スキル受付完了: " + skillId);
            }
        }
    }

    @Override
    public void handleDodge(Player player) {
        if (player == null) return;

        // EMERGENCYスキルのインデックスを特定
        int index = findSkillIndex(MimicSkillLeads.TEST_EMERGENCY);
        if (index == -1) return;

        // クールダウン判定
        if (getCooldown(index) > 0) return;

        // 1. スキルとしての発動（パケット送信とキャンセル処理）
        if (player.level().isClientSide()) {
            SkillLead lead = SkillLeadRegistry.getNullable(MimicSkillLeads.TEST_EMERGENCY);
            if (lead != null) {
                this.abilityCooldowns[index] = lead.totalPreviewTicks + COOLDOWNS[index];
                // 予兆表示（EMERGENCYは通常、一瞬で終わる）
                MathMain math = SkillLeadUtil.buildMath(lead, player.position());
                ClientEvents.spawnLocal(player, lead, math);
                ModMessages.INSTANCE.sendToServer(new C2S_SkillCastRequestPacket(MimicSkillLeads.TEST_EMERGENCY));
            }
        } else {
            this.abilityCooldowns[index] = 1;
        }

        // 2. 即時ワープ処理を実行
        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        // 向いている方向の逆に飛ばしたい場合は +sin / -cos に調整してください
        Vec3 target = player.position().add(-Math.sin(rad) * 15.0, 0, Math.cos(rad) * 15.0);
        player.setPos(target);
        player.setDeltaMovement(Vec3.ZERO);

        // サーバー側からクライアントへワープ位置を同期
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), new S2CMimicDodgePacket(player.getId(), target));
        }
    }

    public void startActualCooldown(int index) {
        if (index >= 0 && index < COOLDOWNS.length) {
            this.abilityCooldowns[index] = COOLDOWNS[index];
            System.out.println("[サーバー] 正式クールダウン開始: " + SKILLS[index]);
        }
    }

    @Override
    public int findSkillIndex(SkillId skillId) {
        if (skillId == null) return -1;
        String searchTarget = skillId.toString();
        for (int i = 0; i < SKILLS.length; i++) {
            if (SKILLS[i].toString().equals(searchTarget)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void tickServer(Player player) {
        super.tickServer(player);
    }

    public void tickClient(Player player) {
        updateCooldowns();
    }

    @Override
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        if (getEntity() instanceof MimicEntity mimic) tag.putBoolean("isOpen", mimic.isOpen());
        tag.putIntArray("cooldowns", abilityCooldowns);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        if (getEntity() instanceof MimicEntity mimic && tag.contains("isOpen")) mimic.setOpen(tag.getBoolean("isOpen"));
        if (tag.contains("cooldowns")) {
            int[] cd = tag.getIntArray("cooldowns");
            for (int i = 0; i < abilityCooldowns.length && i < cd.length; i++) {
                abilityCooldowns[i] = cd[i];
            }
        }
    }
}