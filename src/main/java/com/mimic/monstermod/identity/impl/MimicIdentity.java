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

    private static final int[] COOLDOWNS = {
            40,
            80,
            160,
            200
    };

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILLS.length);
    }

    /* ============================================================ */
    @Override
    public void handleAbility(Player player, int skillIndex) {

        if (skillIndex < 0 || skillIndex >= SKILLS.length) return;

        SkillId skillId = SKILLS[skillIndex];

        // =========================
        // CLIENT
        // =========================
        if (player.level().isClientSide()) {

            // クールダウン中なら何もせず即 return
            if (abilityCooldowns[skillIndex] > 0) return;

            // サーバーにリクエストを送る前に SkillLead を取得
            SkillLead lead = SkillLeadRegistry.getNullable(skillId);
            if (lead == null) return;

            // MathMain を生成してプレビューを出す
            MathMain math = SkillLeadUtil.buildMath(lead, player.position());
            ClientEvents.spawnLocal(player, lead, math);

            // サーバーにスキル発動リクエスト
            ModMessages.INSTANCE.sendToServer(
                    new C2S_SkillCastRequestPacket(skillId)
            );

            return;
        }

        // =========================
        // SERVER
        // =========================
        // クールダウン中なら何もせず即 return
        if (abilityCooldowns[skillIndex] > 0) return;

        // クールダウンをリセット
        abilityCooldowns[skillIndex] = COOLDOWNS[skillIndex];
    }
    /* ============================================================ */

    @Override
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {

        if (menuKey) handleMenu(player);

        if (useKey && skillIndex >= 0) {
            handleAbility(player, skillIndex);
        }
    }

    /* ============================================================ */

    @Override
    public void handleDodge(Player player) {

        if (player == null) return;

        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);

        Vec3 target = player.position().add(
                -Math.sin(rad) * 15.0,
                0,
                Math.cos(rad) * 15.0
        );

        player.setPos(target);
        player.setDeltaMovement(Vec3.ZERO);

        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {

            ModMessages.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new S2CMimicDodgePacket(player.getId(), target)
            );
        }
    }

    /* ============================================================ */

    @Override
    public void tickServer(Player player) {
        super.tickServer(player);
    }

    /* ============================================================ */

    @Override
    public CompoundTag serializeNBT() {

        CompoundTag tag = super.serializeNBT();

        if (getEntity() instanceof MimicEntity mimic) {
            tag.putBoolean("isOpen", mimic.isOpen());
        }

        tag.putIntArray("cooldowns", abilityCooldowns);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {

        super.deserializeNBT(tag);

        if (getEntity() instanceof MimicEntity mimic && tag.contains("isOpen")) {
            mimic.setOpen(tag.getBoolean("isOpen"));
        }

        if (tag.contains("cooldowns")) {

            int[] cd = tag.getIntArray("cooldowns");

            for (int i = 0; i < abilityCooldowns.length && i < cd.length; i++) {
                abilityCooldowns[i] = cd[i];
            }
        }
    }
}