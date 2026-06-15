package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.network.client.C2S_SkillCastRequestPacket;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.skill.hunter.HunterSkill.HunterSkillSlot;
import com.mimic.monstermod.skill.hunter.HunterSkillRegistry;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;

public class HunterIdentity extends BaseIdentity {
    public HunterIdentity(@Nullable BaseEntity entity, int abilityCount) {
        super(entity, 4);
    }

    /**
     * 【修正版】CapabilityのMapからスキルIDとCDを配列に同期する
     */
    public void syncSkillsFromCapability(Map<HunterSkillSlot, SkillId> skills) {
        System.out.println("[Debug] Syncing skills to Identity...");
        for (var entry : skills.entrySet()) {
            int index = entry.getKey().ordinal();
            if (index >= 0 && index < this.skillIds.length) {
                this.skillIds[index] = entry.getValue();
                System.out.println("[Debug] Set index " + index + " to " + entry.getValue());

                if (this.skillIds[index] != null) {
                    HunterSkill hs = HunterSkillRegistry.get(this.skillIds[index]);
                    if (hs != null) {
                        this.defaultCooldowns[index] = hs.getCooldownTicks();
                    }
                }
            }
        }
    }

    @Override
    public int findSkillIndex(SkillId skillId) {
        if (skillId == null) return -1;
        for (int i = 0; i < this.skillIds.length; i++) {
            if (skillId.equals(this.skillIds[i])) return i;
        }
        return -1;
    }

    // 外部（Packetなど）から明示的にプレイヤー経由で最新状態にする場合
    public void forceRefreshSkills(Player player) {
        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
            for (int i = 0; i < 4; i++) {
                HunterSkillSlot slot = HunterSkillSlot.values()[i];
                SkillId id = cap.getEquippedSkill(slot);
                this.skillIds[i] = id;
                if (id != null) {
                    HunterSkill hs = HunterSkillRegistry.get(id);
                    if (hs != null) this.defaultCooldowns[i] = hs.getCooldownTicks();
                }
            }
        });
    }

    @Override
    public boolean isEffectActive(int index) {
        if (index < 0 || index >= 4) return false;

        int lockTime = lockCooldowns[index];
        if (lockTime <= 0) return false;

        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return false;

        SkillId currentSkillId = player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .map(cap -> cap.getEquippedSkill(HunterSkillSlot.values()[index]))
                .orElse(null);

        if (currentSkillId == null) return false;

        SkillLead lead = SkillLeadRegistry.getNullable(currentSkillId);
        if (lead == null) return false;

        if (lead.effectTicks <= 1) return false;

        int effectEnd = lead.recoveryTicks + 1;
        int effectStart = effectEnd + lead.effectTicks;

        return lockTime <= effectStart && lockTime > effectEnd;
    }

    public void serverExecuteSkill(ServerPlayer player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= 4) return;

        SkillId id = this.skillIds[skillIndex];
        if (id == null) {
            // ここでログが出る場合は syncSkillsFromCapability が正しく呼ばれていないことを意味します
            System.out.println("[Debug] serverExecuteSkill: SkillId is null at index " + skillIndex);
            return;
        }

        if (abilityCooldowns[skillIndex] > 0 || lockCooldowns[skillIndex] > 0) return;

        HunterSkill hunterSkill = HunterSkillRegistry.get(id);
        if (hunterSkill != null) {
            boolean isSheathed = player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .map(cap -> cap.isSheathed()).orElse(true);
            if (hunterSkill.getAllowedState() == HunterSkill.SheathState.SHEATHED_ONLY && !isSheathed) return;
            if (hunterSkill.getAllowedState() == HunterSkill.SheathState.DRAWN_ONLY && isSheathed) return;
        }

        SkillLead lead = SkillLeadRegistry.getNullable(id);
        if (lead == null || !canCastByCategory(lead)) return;

        C2S_SkillCastRequestPacket.processSkillCast(player, this, id);
        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> cap.syncToClient(player));
    }

    @Override
    public void handleAbility(Player player, int skillIndex) {
        if (skillIds == null || skillIndex < 0 || skillIndex >= skillIds.length) return;

        SkillId skillId = skillIds[skillIndex];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        if (player.level().isClientSide()) {
            if (abilityCooldowns[skillIndex] > 0 || lockCooldowns[skillIndex] > 0) return;
            if (!canCastByCategory(lead)) return;

            this.lockCooldowns[skillIndex] = 20;

            com.mimic.monstermod.network.ModMessages.INSTANCE.sendToServer(
                    new C2S_SkillCastRequestPacket(skillId)
            );
        }
    }

    private boolean canCastByCategory(SkillLead lead) {
        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.CANCEL) return true;
        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.COMBO) return isComboWindowActive();
        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.NORMAL) {
            boolean isDashing = false;
            for (int i = 0; i < skillIds.length; i++) {
                SkillLead l = SkillLeadRegistry.getNullable(skillIds[i]);
                if (l != null && l.category == com.mimic.monstermod.skill.SkillType.Category.DASH && comboWindows[i] > 0) {
                    isDashing = true;
                    break;
                }
            }
            return !isAnySkillActive() || isDashing;
        }
        return !isAnySkillActive();
    }
}