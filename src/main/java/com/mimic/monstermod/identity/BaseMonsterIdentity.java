package com.mimic.monstermod.identity;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.skill.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public class BaseMonsterIdentity {

    protected final String id;
    @Nullable
    protected final BaseMonsterEntity entity;
    protected int[] abilityCooldowns;
    protected int[] lockCooldowns; // 予兆中フラグ兼タイマー
    protected com.mimic.monstermod.skill.SkillId[] skillIds = new com.mimic.monstermod.skill.SkillId[0];
    protected int[] defaultCooldowns = new int[0];

    public BaseMonsterIdentity(@Nullable BaseMonsterEntity entity, int abilityCount) {
        this.entity = entity;
        this.abilityCooldowns = new int[abilityCount];
        this.lockCooldowns = new int[abilityCount];
        this.id = entity != null ? entity.getType().toString() : "unknown";
    }

    public String getId() { return id; }
    @Nullable public BaseMonsterEntity getEntity() { return entity; }
    public SkillId[] getSkillIds() { return this.skillIds; }

    /**
     * 指定したスキルの予兆ロック中かチェック
     */
    public boolean isLocking(int index) {
        if (index < 0 || index >= lockCooldowns.length) return false;
        return lockCooldowns[index] > 0;
    }

    public void tickServer(Player player) {
        updateCooldowns(player, "サーバー");
        copyFromPlayerServer(player);
    }

    public void tickClient(Player player) {
        updateCooldowns(player, "クライアント");
        copyFromPlayerClient(player);
    }
    public int getDefaultCooldown(int index) {
        if (index < 0 || index >= defaultCooldowns.length) return 0;
        return defaultCooldowns[index];
    }
    public void updateCooldowns(Player player, String side) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            // 1. 予兆ロック減算
            if (lockCooldowns[i] > 0) {
                lockCooldowns[i]--;
                continue; // ロック中はリロードを進めない
            }

            // 2. リロード減算
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
                if (abilityCooldowns[i] == 0) {
                    SkillId id = (i < skillIds.length) ? skillIds[i] : null;
                    System.out.println("[" + side + "] スキル使用可能: " + (id != null ? id : "Index " + i));
                }
            }
        }
    }

    public boolean isAnyNormalSkillActive() {
        for (int i = 0; i < skillIds.length; i++) {
            SkillLead lead = SkillLeadRegistry.getNullable(skillIds[i]);
            if (lead != null && lead.category == SkillType.Category.NORMAL) {
                if (lockCooldowns[i] > 0) return true;
            }
        }
        return false;
    }

    public boolean isAnySkillActive() {
        for (int i = 0; i < lockCooldowns.length; i++) {
            if (lockCooldowns[i] > 0) return true;
        }
        return false;
    }

    /**
     * スキル発動処理の完全版。
     * EMERGENCY 以外は、他のスキルが動作している間は発動をガードします。
     */
    /**
     * スキル発動処理の完全版。
     * NORMALカテゴリのみ、他のスキルが動作している間は発動をガードします。
     * COMBO と CANCELは動作中でも発動可能です。
     */
    public void handleAbility(Player player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= skillIds.length) return;
        SkillId skillId = skillIds[skillIndex];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        // CANCELなら他スキルの予兆を強制キャンセル
        if (lead.category == SkillType.Category.CANCEL) {
            for (int i = 0; i < lockCooldowns.length; i++) {
                lockCooldowns[i] = 0;
                if (i < defaultCooldowns.length && abilityCooldowns[i] > defaultCooldowns[i]) {
                    abilityCooldowns[i] = defaultCooldowns[i];
                }
            }
        }

        // --- 二重発動・割り込み防止 ---
        // 1. 自身のスキルが既に実行待ち（クールダウン中、または自分自身の予兆中）なら無視
        if (lockCooldowns[skillIndex] > 0 || abilityCooldowns[skillIndex] > 0) return;

        // 2. NORMALスキルのみの制限: 他のスキルが何かしら動いているなら、発動を拒否
        if (lead.category == SkillType.Category.NORMAL && isAnySkillActive()) {
            return;
        }

        // --- タイマーセット ---
        this.lockCooldowns[skillIndex] = lead.totalPreviewTicks;
        this.abilityCooldowns[skillIndex] = (skillIndex < defaultCooldowns.length) ? defaultCooldowns[skillIndex] : 60;

        if (player.level().isClientSide()) {
            MathMain math = SkillLeadUtil.buildMath(lead, player.position());
            com.mimic.monstermod.events.PreviewEvents.spawnLocal(player, lead, math);
            com.mimic.monstermod.network.ModMessages.INSTANCE.sendToServer(new com.mimic.monstermod.network.client.C2S_SkillCastRequestPacket(skillId));
        } else {
            // サーバー側：即時スキル実行（予兆なしスキルの場合）
            if (lead.totalPreviewTicks <= 0) {
                SkillEffectSpec spec = SkillEffectRegistry.getNullable(skillId);
                if (spec != null) spec.apply(player, null);
            }
        }
    }

    public int findSkillIndex(com.mimic.monstermod.skill.SkillId skillId) {
        if (skillId == null) return -1;
        for (int i = 0; i < skillIds.length; i++) {
            if (skillIds[i].equals(skillId)) return i;
        }
        return -1;
    }

    public int getCooldown(int index) {
        if (index < 0 || index >= abilityCooldowns.length) return 0;
        return abilityCooldowns[index];
    }

    // --- 同期/描画系（省略せずそのまま保持） ---
    public void copyFromPlayerServer(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
    }

    public void copyFromPlayerClient(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.tickCount = player.tickCount;
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        if (moving) entity.walkAnimation.update((float) player.getDeltaMovement().horizontalDistance(), 1.0f);
        entity.setPlayerActiveMove(moving);
        entity.setSprinting(player.isSprinting());
        entity.setShiftKeyDown(player.isCrouching());
    }

    public void copyRotationPoseAndEquip(Player player) {
        if (entity == null) return;
        entity.yBodyRot = player.yBodyRot;
        entity.yBodyRotO = player.yBodyRotO;
        entity.setYRot(player.getYRot());
        entity.setXRot(player.getXRot());
        float relativeHeadYaw = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
        entity.setYHeadRot(player.yBodyRot + relativeHeadYaw);
        entity.yRotO = player.yRotO;
        entity.xRotO = player.xRotO;
        entity.yHeadRotO = player.yBodyRotO + Mth.wrapDegrees(player.yHeadRotO - player.yBodyRotO);
        entity.setPose(player.getPose());
        for (EquipmentSlot slot : EquipmentSlot.values()) entity.setItemSlot(slot, player.getItemBySlot(slot));
    }

    public void render(Player player, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity == null) return;
        float bodyYaw = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).render(entity, bodyYaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();
    }

    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }
    public void handleMenu(Player player) {}

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putIntArray("cooldowns", abilityCooldowns);
        tag.putIntArray("lock_cooldowns", lockCooldowns);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("cooldowns")) {
            int[] saved = tag.getIntArray("cooldowns");
            for (int i = 0; i < abilityCooldowns.length && i < saved.length; i++) abilityCooldowns[i] = saved[i];
        }
        if (tag.contains("lock_cooldowns")) {
            int[] saved = tag.getIntArray("lock_cooldowns");
            for (int i = 0; i < lockCooldowns.length && i < saved.length; i++) lockCooldowns[i] = saved[i];
        }
    }
}