package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.skill.hunter.HunterSkill.HunterSkillSlot;
import com.mimic.monstermod.skill.hunter.HunterSkillRegistry;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class HunterIdentity extends BaseIdentity {

    public HunterIdentity(@Nullable BaseEntity entity, int abilityCount) {
        super(entity, 4); // ハンターは4枠固定
        // 最初の一回だけセット
        if (entity != null) {
            Player player = entity.level().getNearestPlayer(entity, -1);
            if (player != null) {
                forceRefreshSkills(player);
            }
        }
    }

    // 外部（Packetなど）からスキルが変わったことを通知された時だけ呼ぶ
    public void forceRefreshSkills(Player player) {
        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
            for (int i = 0; i < 4; i++) {
                HunterSkillSlot slot = HunterSkillSlot.values()[i];
                SkillId id = cap.getEquippedSkill(slot);
                this.skillIds[i] = id;
                if (id != null) {
                    HunterSkill hs = HunterSkillRegistry.get(id);
                    // ここでセットしておけば、あとはBaseIdentityがこの値を参照してCDを計算する
                    this.defaultCooldowns[i] = hs.getCooldownTicks();
                }
            }
        });
    }

    @Override
    public int findSkillIndex(SkillId skillId) {
        if (skillId == null) return -1;

        // entity経由ではなく、Minecraftインスタンスや配布されたplayerを使うのが確実
        // クライアント側なら Minecraft.getInstance().player、サーバー側なら entity 参照
        Player player = null;
        if (this.entity != null) {
            player = entity.level().getNearestPlayer(entity, -1);
        } else {
            // クライアント側でのフォールバック
            player = net.minecraft.client.Minecraft.getInstance().player;
        }

        if (player == null) return -1;

        return player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).map(cap -> {
            for (int i = 0; i < 4; i++) {
                HunterSkillSlot slot = HunterSkillSlot.values()[i];
                if (skillId.equals(cap.getEquippedSkill(slot))) return i;
            }
            return -1;
        }).orElse(-1);
    }
    @Override
    public boolean isEffectActive(int index) {
        if (index < 0 || index >= 4) return false;

        int lockTime = lockCooldowns[index];
        if (lockTime <= 0) return false;

        // BaseIdentity の skillIds[] ではなく、Capability から直接 ID を取得する
        // これによりクライアント側でも確実に SkillLead を参照できるようになる
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return false;

        SkillId currentSkillId = player.getCapability(com.mimic.monstermod.variable.CapabilityRegistry.HUNTER_TRANSFORMATION)
                .map(cap -> cap.getEquippedSkill(com.mimic.monstermod.skill.hunter.HunterSkill.HunterSkillSlot.values()[index]))
                .orElse(null);

        if (currentSkillId == null) return false;

        SkillLead lead = SkillLeadRegistry.getNullable(currentSkillId);
        if (lead == null) return false;

        if (lead.effectTicks <= 1) return false;

        // タイムライン計算（ここは BaseIdentity と同じロジック）
        int effectEnd = lead.recoveryTicks + 1;
        int effectStart = effectEnd + lead.effectTicks;

        return lockTime <= effectStart && lockTime > effectEnd;
    }
    @Override
    public void handleAbility(Player player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= skillIds.length) return;

        // 1. スキルIDと最大CDを最新状態にする（保険）
// パケット側で特定された最新のIDを、Capabilityから取得して配列に即座に反映
        player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
            SkillId equippedId = cap.getEquippedSkill(HunterSkillSlot.values()[skillIndex]);
            this.skillIds[skillIndex] = equippedId;

            // CDの最大値を更新（これをやらないとHUDの「黒い幕」の比率がバグるため重要）
            if (equippedId != null) {
                HunterSkill hs = HunterSkillRegistry.get(equippedId);
                if (hs != null) this.defaultCooldowns[skillIndex] = hs.getCooldownTicks();
            }
        });

        SkillId currentSkillId = this.skillIds[skillIndex];
        if (currentSkillId == null) return;

        SkillLead lead = SkillLeadRegistry.getNullable(currentSkillId);
        if (lead == null) return;

        // 2. CD・ロック中チェック
        if (abilityCooldowns[skillIndex] > 0 || lockCooldowns[skillIndex] > 0) return;

        // 3. ハンター専用条件チェック（抜刀・納刀）
        HunterSkill hunterSkill = HunterSkillRegistry.get(currentSkillId);
        if (hunterSkill != null) {
            boolean isSheathed = player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .map(cap -> cap.isSheathed()).orElse(true);
            HunterSkill.SheathState allowed = hunterSkill.getAllowedState();
            if (allowed == HunterSkill.SheathState.SHEATHED_ONLY && !isSheathed) return;
            if (allowed == HunterSkill.SheathState.DRAWN_ONLY && isSheathed) return;
        }

        // 4. カテゴリ別判定
        if (!canCastByCategory(lead)) return;

        // 5. 実行処理
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            com.mimic.monstermod.Math.MathMain math = com.mimic.monstermod.skill.SkillLeadUtil.buildMath(lead, sp.position());

            if (com.mimic.monstermod.skill.SkillUtil.tryExecute(sp.serverLevel(), sp, lead, math)) {
                // 親のロジックでサーバー側のCD配列をセット
                super.handleAbility(player, skillIndex);

                // ★重要：セットした瞬間のCD情報をクライアントへ同期する
                player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
                    cap.syncToClient(sp);
                });

                // 演出用パケット送信
                com.mimic.monstermod.network.ModMessages.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                        new com.mimic.monstermod.network.server.S2C_SpawnSkillLeadPacket(sp.getId(), lead, math)
                );
            }
        } else {
            // クライアント側（予測）
            super.handleAbility(player, skillIndex);
        }
    }

    /**
     * SkillCategoryに基づいた発動可否判定 (BaseIdentityのロジックをカプセル化)
     */
    private boolean canCastByCategory(SkillLead lead) {
        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.CANCEL) return true;

        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.COMBO) {
            return isComboWindowActive();
        }

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

        // DASH, UNIQUEなどは他のスキルが動いていないことが条件
        return !isAnySkillActive();
    }
}