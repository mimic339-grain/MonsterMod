package com.mimic.monstermod.capability;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CHunterSyncPacket;
import com.mimic.monstermod.util.HunterUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HunterTransformation {

    private ItemStack equippedWeapon = ItemStack.EMPTY;
    private String weaponType = "";
    private boolean isSheathed = true;
    private float moveSpeedPenalty = 0.0f;
    private boolean isActive = false;

    // 攻撃・コンボ
    private int comboCount = 0;
    private float attackStiffness = 0f;

    // ================================================================
    // Getters
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
        syncToClient(player);
    }

    public void stopHunter(Player player) {
        if (player == null) return;
        isActive = false;
        if (!isSheathed) setSheathed(player, true);
        resetCombo();
        attackStiffness = 0f;
        syncToClient(player);
    }

    // ================================================================
    // 装備変更
    // ================================================================
    public void equipWeapon(Player player, ItemStack stack, String type) {
        if (player == null) return;

        this.equippedWeapon = stack.copy();
        this.weaponType = type;

        if (!isSheathed) HunterUtil.applyLayerWeapon(player, equippedWeapon);
        isActive = !stack.isEmpty();
        syncToClient(player);
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

        syncToClient(player);
    }

    private void sheatheWeapon(Player player) {
        HunterUtil.removeHandWeaponLayer(player);
        HunterUtil.enableHotbarRender(player);
        HunterUtil.removeMovePenalty(player, moveSpeedPenalty);
    }

    private void unsheatheWeapon(Player player) {
        HunterUtil.applyLayerWeapon(player, equippedWeapon);
        HunterUtil.disableHotbarRender(player);
        HunterUtil.applyMovePenalty(player, moveSpeedPenalty);
    }
    public void sheathWeapon(Player player) {
        this.isSheathed = true;
        // 実際の描画やペナルティはテスト段階ではログだけ
        System.out.println(player.getName().getString() + " sheath weapon");
    }

    public void unsheathWeapon(Player player) {
        this.isSheathed = false;
        System.out.println(player.getName().getString() + " unsheath weapon");
    }
    // ================================================================
    // 攻撃・コンボ
    // ================================================================
    public void addAttackStiffness(float time) { attackStiffness = time; }
    public void resetCombo() { comboCount = 0; }
    public void increaseCombo() { comboCount = (comboCount + 1) % 3; }

    // ================================================================
    // アクションごとのアニメーション名取得（固定）
    // ================================================================
// 回避アクション
    public String getDodgeAnimationName() {return "hammer_idle";}
    // 納刀アクション
    public String getSheathAnimationName() {return "hammer_idle2";}
    // 抜刀アクション
    public String getDrawAnimationName() {return "hammer_idle3";}
    // スキル1アクショ
    public String getSkill1AnimationName() {return "hammer_idle4";}
    // スキル2アクション
    public String getSkill2AnimationName() {return "hammer_idle5";}
    // スキル3アクション
    public String getSkill3AnimationName() {return "hammer_idle6";
    }// ================================================================
    // Sync
    // ================================================================
    public void syncToClient(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.sendToClient(new S2CHunterSyncPacket(player.getUUID(), nbt), sp);
    }

    public void syncToAll(Player player) {
        if (player.level().isClientSide) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.sendToAllClients(new S2CHunterSyncPacket(player.getUUID(), nbt));
    }

    // ================================================================
    // NBT
    // ================================================================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Sheathed", isSheathed);
        tag.putString("WeaponType", weaponType);
        tag.putFloat("MovePenalty", moveSpeedPenalty);
        tag.putBoolean("IsActive", isActive);
        tag.putInt("ComboCount", comboCount);
        tag.putFloat("AttackStiffness", attackStiffness);

        CompoundTag stackTag = new CompoundTag();
        equippedWeapon.save(stackTag);
        tag.put("WeaponStack", stackTag);

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

        if (tag.contains("WeaponStack")) equippedWeapon = ItemStack.of(tag.getCompound("WeaponStack"));
        else equippedWeapon = ItemStack.EMPTY;
    }

    public void onLoad(Player player) {
        if (!isSheathed && isActive) HunterUtil.applyLayerWeapon(player, equippedWeapon);
    }
}
