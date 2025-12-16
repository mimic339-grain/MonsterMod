package com.mimic.monstermod.capability;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CHunterSyncPacket;
import com.mimic.monstermod.util.HunterUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
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
    // Hunter専用 WeaponSlot（★Inventory外の実体★）
    // ================================================================
    private ItemStack weaponSlot = ItemStack.EMPTY;

    /* ================================================================
     * Getter
     * ================================================================ */

    public ItemStack getWeaponSlot() {
        return weaponSlot;
    }

    public ItemStack getEquippedWeapon() { return equippedWeapon; }
    public String getWeaponType() { return weaponType; }
    public boolean isSheathed() { return isSheathed; }
    public float getPenalty() { return moveSpeedPenalty; }
    public boolean isActive() { return isActive; }
    public int getComboCount() { return comboCount; }
    public float getAttackStiffness() { return attackStiffness; }

    public static boolean isHunter(Player player) {
        if (player == null) return false;
        return player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .map(HunterTransformation::isActive)
                .orElse(false);
    }

    /* ================================================================
     * WeaponSlot 制御（★重要★）
     * ================================================================ */
    /** サーバー権威で WeaponSlot を更新 */
    public void setWeaponSlotServer(ItemStack stack, ServerPlayer player) {
        if (!canAcceptWeapon(stack)) return;
        // ★ ログイン完了前は送らない
        if (player.connection == null) return;
        this.weaponSlot = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        this.weaponSlot.setCount(1);

        // デバッグログ
        MonsterMod.LOGGER.info("[WeaponSlot][SERVER] setWeaponSlotServer: {} x{} (player={})",
                this.weaponSlot.isEmpty() ? "EMPTY" : this.weaponSlot.getItem().toString(),
                this.weaponSlot.getCount(),
                player.getGameProfile().getName()
        );

        syncToClient(player);
    }

    /** 受け入れ判定（サーバー側） */
    public boolean canAcceptWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;

        boolean accepted = stack.getItem() instanceof WeaponItem;

        return accepted;
    }

    // client 専用
    public void setWeaponSlotClient(ItemStack stack) {
        this.weaponSlot = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    /** WeaponSlot → 現在装備へ反映 */
    public void syncEquippedFromSlot(Player player) {
        if (player == null) return;
        if (weaponSlot.isEmpty()) return;

        this.equippedWeapon = weaponSlot.copy();
        this.isActive = true;

        if (!isSheathed)
            HunterUtil.applyLayerWeapon(player, equippedWeapon);

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }
    /* ================================================================
     * Hunter 開始 / 終了
     * ================================================================ */

    public void startHunter(Player player) {
        if (player == null) return;
        isActive = true;

        if (!isSheathed)
            HunterUtil.applyLayerWeapon(player, equippedWeapon);

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    public void stopHunter(Player player) {
        if (player == null) return;

        isActive = false;
        if (!isSheathed)
            setSheathed(player, true);

        resetCombo();
        attackStiffness = 0f;

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    /* ================================================================
     * 武器変更
     * ================================================================ */

    public void equipWeapon(Player player, ItemStack stack, String type) {
        if (player == null) return;

        this.equippedWeapon = stack.copy();
        this.weaponType = type;
        this.isActive = !stack.isEmpty();

        if (!isSheathed)
            HunterUtil.applyLayerWeapon(player, equippedWeapon);

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    /* ================================================================
     * 納刀 / 抜刀
     * ================================================================ */

    public void setSheathed(Player player, boolean state) {
        if (player == null) return;

        this.isSheathed = state;
        this.isActive = !state && !equippedWeapon.isEmpty();

        if (isSheathed) sheatheWeapon(player);
        else unsheatheWeapon(player);

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
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

    /* ================================================================
     * 攻撃・コンボ
     * ================================================================ */

    public void addAttackStiffness(float time) { attackStiffness = time; }
    public void resetCombo() { comboCount = 0; }
    public void increaseCombo() { comboCount = (comboCount + 1) % 3; }
    /* ================================================================
     * Sync
     * ================================================================ */

    public void syncToClient(ServerPlayer player) {
        CompoundTag nbt = serializeNBT();
        ModMessages.sendToClient(new S2CHunterSyncPacket(player.getUUID(), nbt), player);
    }

    public void syncToAll(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        CompoundTag nbt = serializeNBT();
        ModMessages.sendToAllClients(new S2CHunterSyncPacket(player.getUUID(), nbt));
    }
    // ================================================================
    // アニメーション名
    // ================================================================
    public String getDodgeAnimationName() {return "hammer_idle";}
    public String getSheathAnimationName() {return "hammer_idle2";}
    public String getDrawAnimationName() {return "hammer_idle3";}
    public String getSkill1AnimationName() {return "hammer_idle4";}
    public String getSkill2AnimationName() {return "hammer_idle5";}
    public String getSkill3AnimationName() {return "hammer_idle6";}
    /* ================================================================
     * NBT
     * ================================================================ */

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

        equippedWeapon = tag.contains("WeaponStack")
                ? ItemStack.of(tag.getCompound("WeaponStack"))
                : ItemStack.EMPTY;

        weaponSlot = tag.contains("HunterSlot")
                ? ItemStack.of(tag.getCompound("HunterSlot"))
                : ItemStack.EMPTY;
    }

    /* ================================================================
     * ロード時
     * ================================================================ */

    public void onLoad(Player player) {
        if (!isSheathed && isActive)
            HunterUtil.applyLayerWeapon(player, equippedWeapon);
    }


}
