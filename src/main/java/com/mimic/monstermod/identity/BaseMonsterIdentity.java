package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.Mth;

/**
 * BaseMonsterIdentity 完全版（IDENTITYMOD方式）
 * - サーバーは能力・クールタイム更新
 * - クライアントは毎フレーム Player 状態をコピーして描画
 */
public class BaseMonsterIdentity {

    protected final String id;
    @Nullable
    protected final BaseMonsterEntity entity;
    protected int[] abilityCooldowns;

    public BaseMonsterIdentity(@Nullable BaseMonsterEntity entity, int abilityCount) {
        this.entity = entity;
        this.abilityCooldowns = new int[abilityCount];
        if (entity != null) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            this.id = key != null ? key.toString() : "unknown";
        } else {
            this.id = "unknown";
        }
    }

    public BaseMonsterIdentity(String id, int abilityCount) {
        this.entity = null;
        this.id = id;
        this.abilityCooldowns = new int[abilityCount];
    }

    public String getId() { return id; }
    @Nullable public BaseMonsterEntity getEntity() { return entity; }

    /** BoundingBoxサイズ */
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        if (entity != null) {
            var dims = entity.getDimensions(pose);
            return new Vec3(dims.width, dims.height, dims.width);
        }
        return new Vec3(0.6f, 1.8f, 0.6f);
    }

    /** EyeHeight */
    public float getEyeHeight(Pose pose) {
        if (entity != null) return entity.getEyeHeight(pose);
        return 1.62f;
    }

    /** サーバー専用 Tick（クールタイム・能力更新） */
    public void tickServer(Player player) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;
        }
    }

    /** サーバー用コピー（回転・Pose・装備・位置・速度） */
    public void copyFromPlayerServer(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.setPos(player.getX(), player.getY(), player.getZ());
    }

    /** クライアント用コピー（毎フレーム呼び出し） */
    public void copyFromPlayerClient(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.setPos(player.getX(), player.getY(), player.getZ());
    }

    /** 共通コピー処理 */
    private void copyRotationPoseAndEquip(Player player) {
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

    /** 描画（Tick で同期済みの値を補間） */
    public void render(Player player, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity == null) return;

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(entity)
                .render(entity, yaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();
    }

    /** クライアント入力処理 */
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

        ListTag cooldownList = new ListTag();
        for (int cd : abilityCooldowns) {
            CompoundTag cdTag = new CompoundTag();
            cdTag.putInt("cd", cd);
            cooldownList.add(cdTag);
        }
        tag.put("abilityCooldowns", cooldownList);
        return tag;
    }

    /** NBT復元 */
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("abilityCooldowns")) {
            ListTag cooldownList = tag.getList("abilityCooldowns", 10);
            for (int i = 0; i < cooldownList.size() && i < abilityCooldowns.length; i++) {
                abilityCooldowns[i] = cooldownList.getCompound(i).getInt("cd");
            }
        }
    }

    public int getCooldown(int index) {
        return (index < 0 || index >= abilityCooldowns.length) ? 0 : abilityCooldowns[index];
    }

    public void setCooldown(int index, int ticks) {
        if (index >= 0 && index < abilityCooldowns.length) abilityCooldowns[index] = ticks;
    }
}
