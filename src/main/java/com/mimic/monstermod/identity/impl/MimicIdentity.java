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
            MimicSkillLeads.TEST_3D
    };

    private static final int[] COOLDOWNS = { 1600, 1000, 1210 };

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILLS.length);
    }

    @Override
    public void handleAbility(Player player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= SKILLS.length) return;

        int currentCD = getCooldown(skillIndex);
        SkillId skillId = SKILLS[skillIndex];

        if (player.level().isClientSide()) {
            if (currentCD > 0) return;

            SkillLead lead = SkillLeadRegistry.getNullable(skillId);
            if (lead == null) return;

            // クライアント側で溜め時間を含めた暫定クールダウンを設定
            int totalWait = lead.totalPreviewTicks + COOLDOWNS[skillIndex];
            this.abilityCooldowns[skillIndex] = totalWait;

            System.out.println("[クライアント] スキル予約: " + skillId + " | 合計待機(溜め含む): " + totalWait + " ticks");

            MathMain math = SkillLeadUtil.buildMath(lead, player.position());
            ClientEvents.spawnLocal(player, lead, math);

            ModMessages.INSTANCE.sendToServer(new C2S_SkillCastRequestPacket(skillId));

        } else {
            // サーバー側：実際の攻撃が出るまで「1」を維持して重複発動を防ぐ
            if (this.abilityCooldowns[skillIndex] <= 0) {
                this.abilityCooldowns[skillIndex] = 1;
                System.out.println("[サーバー] スキル受付完了（プレビュー待機中...）: " + skillId);
            }
        }
    }

    /**
     * 指定されたスキルのクールダウンを実際に開始する（SkillUtilから呼ばれる）
     */
    public void startActualCooldown(int index) {
        if (index >= 0 && index < COOLDOWNS.length) {
            this.abilityCooldowns[index] = COOLDOWNS[index];
            System.out.println("[サーバー] 正式クールダウン開始: " + SKILLS[index] + " (" + COOLDOWNS[index] + " ticks)");
        }
    }

    @Override
    public int findSkillIndex(com.mimic.monstermod.skill.SkillId skillId) {
        if (skillId == null) return -1;
        String searchTarget = skillId.toString();
        for (int i = 0; i < SKILLS.length; i++) {
            if (SKILLS[i].toString().equals(searchTarget)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 描画用の同期メソッド。
     * ここでカウントダウンを行うとFPS依存で加速するため、superの呼び出しのみにする。
     */
    @Override
    public void copyFromPlayerClient(Player player) {
        super.copyFromPlayerClient(player);
        // ここにあった processCooldowns は削除しました
    }

    /**
     * サーバー側のTick処理。
     * 親クラスの super.tickServer(player) が内部で一度だけカウントダウンを行います。
     */
    @Override
    public void tickServer(Player player) {
        // カウントダウン前の値を保持（ログ用）
        int[] prevCD = abilityCooldowns.clone();

        super.tickServer(player);

        // ログ出力処理（0になった瞬間だけ表示）
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (prevCD[i] > 0 && abilityCooldowns[i] == 0) {
                System.out.println("[サーバー] クールダウン終了: " + SKILLS[i]);
            }
        }
    }

    /**
     * クライアント側で ClientEvents (ClientTickEvent) から呼び出す専用メソッド。
     */
    public void tickClient(Player player) {
        int[] prevCD = abilityCooldowns.clone();

        // 親クラスが持つ共通のカウントダウン処理を1回だけ実行
        updateCooldowns();

        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (prevCD[i] > 0 && abilityCooldowns[i] == 0) {
                System.out.println("[クライアント] クールダウン終了: " + SKILLS[i]);
            }
        }
    }

    @Override
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    @Override
    public void handleDodge(Player player) {
        if (player == null) return;
        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        Vec3 target = player.position().add(-Math.sin(rad) * 15.0, 0, Math.cos(rad) * 15.0);
        player.setPos(target);
        player.setDeltaMovement(Vec3.ZERO);
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), new S2CMimicDodgePacket(player.getId(), target));
        }
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
            for (int i = 0; i < abilityCooldowns.length && i < cd.length; i++) abilityCooldowns[i] = cd[i];
        }
    }
}