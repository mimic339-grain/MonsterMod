package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseMonsterEntity;
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
        updateCooldowns();
    }
    // サーバー・クライアント共通のTick処理
    public void updateCooldowns() {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
            }
        }
    }
    public int getCooldown(int index) {
        if (index < 0 || index >= abilityCooldowns.length) return 0;
        return abilityCooldowns[index];
    }
    // 共通で使えるように空のメソッドを定義（MimicIdentityでオーバーライドする）
    public int findSkillIndex(com.mimic.monstermod.skill.SkillId skillId) {
        return -1;
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
        if (player.isShiftKeyDown()) handleDodge(player); // 例えば Shift で回避（クライアント側）
    }

    public void handleAbility(Player player, int skillIndex) {
        if (abilityCooldowns[skillIndex] > 0) return;
        // サーバーでスキル処理
    }

    public void handleMenu(Player player) {}

    /**
     * 共通回避処理（Monster / Hunter 共通）
     * 各 Identity はオーバーライドして固有挙動を実装
     */
    public void handleDodge(Player player) {
        if (entity == null) return;
        // デフォルトは横に小さく移動する簡易回避
        double dx = Math.sin(Math.toRadians(player.getYRot()));
        double dz = -Math.cos(Math.toRadians(player.getYRot()));
        entity.setDeltaMovement(entity.getDeltaMovement().add(dx, 0, dz));
    }

    // -----------------------------
    // NBT 保存 / 復元
    // -----------------------------
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        for (int i = 0; i < abilityCooldowns.length; i++) {
            tag.putInt("cd_" + i, abilityCooldowns[i]);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (tag.contains("cd_" + i)) abilityCooldowns[i] = tag.getInt("cd_" + i);
        }
    }
}
