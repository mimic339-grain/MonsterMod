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

/**
 * BaseMonsterIdentity (上位互換版)
 *
 * - プレイヤー変身状態をBaseMonsterEntityに完全同期
 * - 歩行アニメーションを直接更新
 * - スプリント・スニーク・座標・装備・回転・回避すべて反映
 */
public class BaseMonsterIdentity {

    protected final String id;
    @Nullable
    protected final BaseMonsterEntity entity;
    protected int[] abilityCooldowns;
    protected com.mimic.monstermod.skill.SkillId[] skillIds = new com.mimic.monstermod.skill.SkillId[0];
    protected int[] defaultCooldowns = new int[0];

    public BaseMonsterIdentity(@Nullable BaseMonsterEntity entity, int abilityCount) {
        this.entity = entity;
        this.abilityCooldowns = new int[abilityCount];
        this.id = entity != null ? entity.getType().toString() : "unknown";
    }

    public String getId() { return id; }
    @Nullable public BaseMonsterEntity getEntity() { return entity; }

    // -----------------------------
    // サーバー Tick: クールダウンのみ
    // -----------------------------
    public void tickServer(Player player) {
        updateCooldowns(player, "サーバー");
        copyFromPlayerServer(player); // 座標・装備の同期を自動実行
    }

    public void tickClient(Player player) {
        updateCooldowns(player, "クライアント");
        copyFromPlayerClient(player); // アニメーション・スプリント状態の同期を自動実行
    }

    public void updateCooldowns(Player player, String side) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
                // ちょうど0になった瞬間だけログを出す
                if (abilityCooldowns[i] == 0) {
                    SkillId id = (i < skillIds.length) ? skillIds[i] : null;
                    System.out.println("[" + side + "] スキル使用可能: " + (id != null ? id : "Index " + i) + " (" + player.getName().getString() + ")");
                }
            }
        }
    }
    /**
     * NORMALカテゴリのスキルが動作中（予兆中）かチェックする共通メソッド
     */
    protected boolean isAnyNormalSkillActive() {
        for (int i = 0; i < skillIds.length; i++) {
            SkillLead lead = SkillLeadRegistry.getNullable(skillIds[i]);
            if (lead != null && lead.category == SkillType.Category.NORMAL) {
                // 残りCDがデフォルト値より大きい = 予兆中
                if (abilityCooldowns[i] > defaultCooldowns[i]) return true;
            }
        }
        return false;
    }
    public int findSkillIndex(com.mimic.monstermod.skill.SkillId skillId) {
        if (skillId == null) return -1;
        for (int i = 0; i < skillIds.length; i++) {
            // .toString() ではなく equals で直接比較する
            if (skillIds[i].equals(skillId)) return i;
        }
        return -1;
    }

    public int getCooldown(int index) {
        if (index < 0 || index >= abilityCooldowns.length) return 0;
        return abilityCooldowns[index];
    }
    // -----------------------------
    // プレイヤー状態をEntityにコピー（サーバー用）
    // -----------------------------
    public void copyFromPlayerServer(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
    }
    // -----------------------------
    // プレイヤー状態をEntityにコピー（クライアント用）
    // -----------------------------
    public void copyFromPlayerClient(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.tickCount = player.tickCount;

        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        if (moving) {
            entity.walkAnimation.update(
                    (float) player.getDeltaMovement().horizontalDistance(),
                    1.0f
            );
        }

        entity.setPlayerActiveMove(moving);
        entity.setSprinting(player.isSprinting());
        entity.setShiftKeyDown(player.isCrouching());
    }

    public void copyRotationPoseAndEquip(Player player) {
        if (entity == null) return;

        // 体回転
        entity.yBodyRot = player.yBodyRot;
        entity.yBodyRotO = player.yBodyRotO;
        entity.setYRot(player.getYRot());
        entity.setXRot(player.getXRot());

        // 頭回転
        float relativeHeadYaw = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
        entity.setYHeadRot(player.yBodyRot + relativeHeadYaw);

        float relativeHeadYawO = Mth.wrapDegrees(player.yHeadRotO - player.yBodyRotO);
        entity.yRotO = player.yRotO;
        entity.xRotO = player.xRotO;
        entity.yHeadRotO = player.yBodyRotO + relativeHeadYawO;

        entity.setPose(player.getPose());

        // 装備コピー
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            entity.setItemSlot(slot, player.getItemBySlot(slot));
        }
    }

    // -----------------------------
    // 描画: partialTicks補間
    // -----------------------------
    public void render(Player player, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity == null) return;

        float bodyYaw = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(entity)
                .render(entity, bodyYaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();
    }

    // -----------------------------
    // クライアント入力処理
    // -----------------------------
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    public void handleAbility(Player player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= skillIds.length) return;
        SkillId skillId = skillIds[skillIndex];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        // EMERGENCYなら他スキルの硬直をキャンセル (ここは共通)
        if (lead.category == SkillType.Category.EMERGENCY) {
            System.out.println("[Identity/Debug] EMERGENCY発動: 硬直をリセットします (" + skillId + ")");
            for (int i = 0; i < abilityCooldowns.length; i++) {
                if (i < defaultCooldowns.length && abilityCooldowns[i] > defaultCooldowns[i]) {
                    abilityCooldowns[i] = defaultCooldowns[i];
                }
            }
        }

        if (player.level().isClientSide()) {
            // クライアント側処理
            if (getCooldown(skillIndex) > 0) return;
            if (lead.category == SkillType.Category.NORMAL && isAnyNormalSkillActive()) return;

            this.abilityCooldowns[skillIndex] = lead.skillTicks + defaultCooldowns[skillIndex];
            MathMain math = SkillLeadUtil.buildMath(lead, player.position());
            com.mimic.monstermod.events.PreviewEvents.spawnLocal(player, lead, math);
            com.mimic.monstermod.network.ModMessages.INSTANCE.sendToServer(new com.mimic.monstermod.network.client.C2S_SkillCastRequestPacket(skillId));
        } else {
            // ★ サーバー側処理: ここが抜けていたポイント！
            this.abilityCooldowns[skillIndex] = lead.skillTicks + defaultCooldowns[skillIndex];

            // EMERGENCYなどの「即時実行」スキルの場合、ここでAttackSpecを適用する
            // NORMALスキルはSkillUtil側のタイマーで後から呼ばれますが、
            // EMERGENCY(Ticks:0) は今すぐ実行する必要があります。
            if (lead.category == SkillType.Category.EMERGENCY) {
                SkillEffectSpec spec = SkillEffectRegistry.getNullable(skillId);
                if (spec != null) {
                    System.out.println("[Server/Identity] 即時スキル(回避等)を実行します: " + skillId);
                    spec.apply(player, null);
                }
            }
        }
    }

    public void handleMenu(Player player) {}


    // -----------------------------
    // NBT共通処理 (int配列の保存を共通化)
    // -----------------------------
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putIntArray("cooldowns", abilityCooldowns);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("cooldowns")) {
            int[] saved = tag.getIntArray("cooldowns");
            for (int i = 0; i < abilityCooldowns.length && i < saved.length; i++) {
                abilityCooldowns[i] = saved[i];
            }
        }
    }
}
