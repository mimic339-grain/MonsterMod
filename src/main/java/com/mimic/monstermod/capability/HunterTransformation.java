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
    // 基本状態
    // ================================================================
    private ItemStack equippedWeapon = ItemStack.EMPTY;
    private ItemStack weaponSlot = ItemStack.EMPTY;

    private String weaponType = "";
    private boolean isSheathed = true;
    private boolean isActive = false;

    private float moveSpeedPenalty = 0.0f;

    // 攻撃関連
    private int comboCount = 0;
    private float attackStiffness = 0f;

    // ================================================================
    // Getter
    // ================================================================
    public ItemStack getWeaponSlot() { return weaponSlot; }
    public ItemStack getEquippedWeapon() { return equippedWeapon; }
    public String getWeaponType() { return weaponType; }
    public boolean isSheathed() { return isSheathed; }
    public boolean isActive() { return isActive; }
    public int getComboCount() { return comboCount; }
    public float getAttackStiffness() { return attackStiffness; }
    public float getPenalty() { return moveSpeedPenalty; }

    public static boolean isHunter(Player player) {
        if (player == null) return false;
        return player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .map(HunterTransformation::isActive)
                .orElse(false);
    }

    // ================================================================
    // WeaponSlot（Inventory外）
    // ================================================================
    public void setWeaponSlotServer(ItemStack stack, ServerPlayer player) {
        if (!canAcceptWeapon(stack)) return;
        if (player.connection == null) return;

        this.weaponSlot = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        this.weaponSlot.setCount(1);

        MonsterMod.LOGGER.info(
                "[WeaponSlot][SERVER] {} x{} ({})",
                weaponSlot.isEmpty() ? "EMPTY" : weaponSlot.getItem(),
                weaponSlot.getCount(),
                player.getGameProfile().getName()
        );

        syncToClient(player);
    }

    public boolean canAcceptWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        return stack.getItem() instanceof WeaponItem;
    }

    // client only
    public void setWeaponSlotClient(ItemStack stack) {
        this.weaponSlot = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    /** WeaponSlot → 現在装備 */
    public void syncEquippedFromSlot(Player player) {
        if (weaponSlot.isEmpty()) return;

        this.equippedWeapon = weaponSlot.copy();
        this.isActive = true;

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    // ================================================================
    // Hunter 開始 / 終了
    // ================================================================
    public void startHunter(Player player) {
        isActive = true;
        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    public void stopHunter(Player player) {
        isActive = false;
        comboCount = 0;
        attackStiffness = 0f;

        if (!isSheathed)
            setSheathed(player, true);

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    // ================================================================
    // 武器変更
    // ================================================================
    public void equipWeapon(Player player, ItemStack stack, String type) {
        this.equippedWeapon = stack.copy();
        this.weaponType = type;

        // isActive は変更しない

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    // ================================================================
    // 納刀 / 抜刀
    // ================================================================
    public void setSheathed(Player player, boolean state) {
        this.isSheathed = state;

        if (!isActive) return; // Hunterでないなら何もしない

        if (isSheathed) {
            HunterUtil.enableHotbarRender(player);
            HunterUtil.removeMovePenalty(player);
        } else {
            HunterUtil.disableHotbarRender(player);
            HunterUtil.applyMovePenalty(player, moveSpeedPenalty);
        }

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    // ================================================================
    // 攻撃・コンボ
    // ================================================================
    public void addAttackStiffness(float time) { attackStiffness = time; }
    public void resetCombo() { comboCount = 0; }
    public void increaseCombo() { comboCount = (comboCount + 1) % 3; }

    // ================================================================
    // Sync
    // ================================================================
    public void syncToClient(ServerPlayer player) {
        ModMessages.sendToClient(
                new S2CHunterSyncPacket(player.getUUID(), serializeNBT()),
                player
        );
    }

    // ================================================================
    // アニメーション名
    // ================================================================
    public String getDodgeAnimationName() { return "sword_simple_idle"; }
    public String getSheathAnimationName() { return "hammer_idle2"; }
    public String getDrawAnimationName() { return "sword_simple_attack1"; }
    public String getSkill1AnimationName() { return "sword_simple_attack2"; }
    public String getSkill2AnimationName() { return "sword_simple_sheathed"; }
    public String getSkill3AnimationName() { return "hammer_idle6"; }

    // ================================================================
    // NBT
    // ================================================================
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Sheathed", isSheathed);
        tag.putBoolean("IsActive", isActive);
        tag.putString("WeaponType", weaponType);
        tag.putFloat("MovePenalty", moveSpeedPenalty);
        tag.putInt("ComboCount", comboCount);
        tag.putFloat("AttackStiffness", attackStiffness);

        CompoundTag eq = new CompoundTag();
        equippedWeapon.save(eq);
        tag.put("EquippedWeapon", eq);

        CompoundTag slot = new CompoundTag();
        weaponSlot.save(slot);
        tag.put("WeaponSlot", slot);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;

        isSheathed = tag.getBoolean("Sheathed");
        isActive = tag.getBoolean("IsActive");
        weaponType = tag.getString("WeaponType");
        moveSpeedPenalty = tag.getFloat("MovePenalty");
        comboCount = tag.getInt("ComboCount");
        attackStiffness = tag.getFloat("AttackStiffness");

        equippedWeapon = tag.contains("EquippedWeapon")
                ? ItemStack.of(tag.getCompound("EquippedWeapon"))
                : ItemStack.EMPTY;

        weaponSlot = tag.contains("WeaponSlot")
                ? ItemStack.of(tag.getCompound("WeaponSlot"))
                : ItemStack.EMPTY;
    }

    // ================================================================
    // Client Load / Sync 後
    // ================================================================
    public void onLoad(Player player) {
        if (!isSheathed && isActive) {
            HunterUtil.disableHotbarRender(player);
            HunterUtil.applyMovePenalty(player, moveSpeedPenalty);
        } else {
            HunterUtil.enableHotbarRender(player);
            HunterUtil.removeMovePenalty(player);
        }
    }
}
