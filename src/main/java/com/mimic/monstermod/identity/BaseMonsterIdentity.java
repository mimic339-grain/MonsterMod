package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

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

    /** サーバー Tick: クールタイムのみ */
    public void tickServer(Player player) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;
        }
    }

    /** サーバー用コピー */
    public void copyFromPlayerServer(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.setPos(player.getX(), player.getY(), player.getZ());
        Vec3 prevPos = entity.position();
        boolean moving = prevPos.distanceToSqr(player.position()) > 0.001;
        entity.setPlayerActiveMove(moving);
    }

    /** クライアント用コピー（毎フレーム） */
    public void copyFromPlayerClient(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);

        Vec3 prevPos = entity.position();
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.setPos(player.getX(), player.getY(), player.getZ());

        boolean moving = prevPos.distanceToSqr(entity.position()) > 0.001;
        entity.setPlayerActiveMove(moving);

        // GeckoLibアニメーション制御
        updateAnimation();
    }

    /** 回転・Pose・装備コピー共通 */
    private void copyRotationPoseAndEquip(Player player) {
        if (entity == null) return;

        entity.setYRot(player.getYRot());
        entity.setXRot(player.getXRot());
        entity.setYHeadRot(player.getYHeadRot());
        entity.yRotO = player.yRotO;
        entity.xRotO = player.xRotO;
        entity.yHeadRotO = player.yHeadRotO;
        entity.setPose(player.getPose());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            entity.setItemSlot(slot, player.getItemBySlot(slot));
        }
    }

    /** GeckoLib アニメーション更新 */
    private void updateAnimation() {
        if (entity == null) return;
        String animName = entity.decideAnimation();
        if (animName != null && !animName.isEmpty()) {
            entity.setAnimation(animName);
        }
    }

    /** 描画（partialTicks補間 + Axis Y回転統合） */
    public void render(Player player, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity == null) return;

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));

        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(entity)
                .render(entity, yaw, partialTicks, poseStack, buffer, light);

        poseStack.popPose();
    }

    /** クライアント入力 */
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    protected void handleAbility(Player player, int skillIndex) {
        if (abilityCooldowns[skillIndex] > 0) return;
    }

    protected void handleMenu(Player player) {}

    /** NBT保存 */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        for (int i = 0; i < abilityCooldowns.length; i++) {
            tag.putInt("cd_" + i, abilityCooldowns[i]);
        }
        return tag;
    }

    /** NBT復元 */
    public void deserializeNBT(CompoundTag tag) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (tag.contains("cd_" + i)) abilityCooldowns[i] = tag.getInt("cd_" + i);
        }
    }
}
