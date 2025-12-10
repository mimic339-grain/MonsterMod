package com.mimic.monstermod.capability;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CHunterSyncPacket;
import com.mimic.monstermod.util.HunterUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HunterTransformation {

    // ================================================================
    // 基本装備情報
    // ================================================================
    private ItemStack equippedWeapon = ItemStack.EMPTY;
    private String weaponType = "";
    private boolean isSheathed = true;
    private float moveSpeedPenalty = 0.0f;
    private boolean isActive = false;

    // 攻撃・コンボ
    private int comboCount = 0;
    private float attackStiffness = 0f;

    // ================================================================
    // Hunter専用スロット
    // ================================================================
    private ItemStack weaponSlot = ItemStack.EMPTY;

    public ItemStack getWeaponSlot() {
        return weaponSlot;
    }

    public void setWeaponSlot(ItemStack stack, Player player) {
        this.weaponSlot = stack.copy();
        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }

    // ================================================================
    // 基本Getters
    // ================================================================
    public ItemStack getEquippedWeapon() { return equippedWeapon; }
    public String getWeaponType() { return weaponType; }
    public boolean isSheathed() { return isSheathed; }
    public float getPenalty() { return moveSpeedPenalty; }
    public boolean isActive() { return isActive; }
    public int getComboCount() { return comboCount; }
    public float getAttackStiffness() { return attackStiffness; }


    // ================================================================
    // Start / Stop Hunter
    // ================================================================
    public void startHunter(Player player) {
        if (player == null) return;
        isActive = true;
        if (!isSheathed) HunterUtil.applyLayerWeapon(player, equippedWeapon);
        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }

    public void stopHunter(Player player) {
        if (player == null) return;
        isActive = false;
        if (!isSheathed) setSheathed(player, true);
        resetCombo();
        attackStiffness = 0f;
        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }


    // ================================================================
    // 武器変更
    // ================================================================
    public void equipWeapon(Player player, ItemStack stack, String type) {
        if (player == null) return;

        this.equippedWeapon = stack.copy();
        this.weaponType = type;

        if (!isSheathed) HunterUtil.applyLayerWeapon(player, equippedWeapon);
        isActive = !stack.isEmpty();

        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }


    // ================================================================
    // 納刀 / 抜刀
    // ================================================================
    public void setSheathed(Player player, boolean state) {
        if (player == null) return;

        this.isSheathed = state;
        this.isActive = !state && !equippedWeapon.isEmpty();

        if (isSheathed) sheatheWeapon(player);
        else unsheatheWeapon(player);

        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }

    public void sheatheWeapon(Player player) {
        HunterUtil.removeHandWeaponLayer(player);
        HunterUtil.enableHotbarRender(player);
        HunterUtil.removeMovePenalty(player, moveSpeedPenalty);
    }

    public void unsheatheWeapon(Player player) {
        HunterUtil.applyLayerWeapon(player, equippedWeapon);
        HunterUtil.disableHotbarRender(player);
        HunterUtil.applyMovePenalty(player, moveSpeedPenalty);
    }


    // ================================================================
    // 攻撃・コンボ
    // ================================================================
    public void addAttackStiffness(float time) { attackStiffness = time; }
    public void resetCombo() { comboCount = 0; }
    public void increaseCombo() { comboCount = (comboCount + 1) % 3; }


    // ================================================================
    // アニメーション名
    // ================================================================
    public String getDodgeAnimationName() {return "hammer_idle";}
    public String getSheathAnimationName() {return "hammer_idle2";}
    public String getDrawAnimationName() {return "hammer_idle3";}
    public String getSkill1AnimationName() {return "hammer_idle4";}
    public String getSkill2AnimationName() {return "hammer_idle5";}
    public String getSkill3AnimationName() {return "hammer_idle6";}


    // ================================================================
    // Sync（★修正版★）
    // ================================================================
    public void syncToClient(ServerPlayer player) {
        CompoundTag nbt = serializeNBT();
        ModMessages.sendToClient(new S2CHunterSyncPacket(player.getUUID(), nbt), player);
    }

    public void syncToAll(Player player) {
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.sendToAllClients(new S2CHunterSyncPacket(player.getUUID(), nbt));
    }


    // ================================================================
    // NBT 保存
    // ================================================================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Sheathed", isSheathed);
        tag.putString("WeaponType", weaponType);
        tag.putFloat("MovePenalty", moveSpeedPenalty);
        tag.putBoolean("IsActive", isActive);
        tag.putInt("ComboCount", comboCount);
        tag.putFloat("AttackStiffness", attackStiffness);

        CompoundTag weaponTag = new CompoundTag();
        equippedWeapon.save(weaponTag);
        tag.put("WeaponStack", weaponTag);

        CompoundTag slotTag = new CompoundTag();
        weaponSlot.save(slotTag);
        tag.put("HunterSlot", slotTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;

        isSheathed = tag.getBoolean("Sheathed");
        weaponType = tag.getString("WeaponType");
        moveSpeedPenalty = tag.getFloat("MovePenalty");
        isActive = tag.getBoolean("IsActive");
        comboCount = tag.getInt("ComboCount");
        attackStiffness = tag.getFloat("AttackStiffness");

        if (tag.contains("WeaponStack"))
            equippedWeapon = ItemStack.of(tag.getCompound("WeaponStack"));
        else
            equippedWeapon = ItemStack.EMPTY;

        if (tag.contains("HunterSlot"))
            weaponSlot = ItemStack.of(tag.getCompound("HunterSlot"));
        else
            weaponSlot = ItemStack.EMPTY;
    }


    // ================================================================
    // ロード時
    // ================================================================
    public void onLoad(Player player) {
        if (!isSheathed && isActive)
            HunterUtil.applyLayerWeapon(player, equippedWeapon);
    }
}
