package com.mimic.monstermod.capability;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.identity.HunterIdentity;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CHunterSyncPacket;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.skill.hunter.HunterSkillRegistry;
import com.mimic.monstermod.util.HunterUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HunterTransformation {
    @Nullable
    private BaseIdentity identity = null;
    @Nullable
    public BaseIdentity getIdentity() { return identity; }
    // ================================================================
    // 基本状態
    // ================================================================
    private ItemStack equippedWeapon = ItemStack.EMPTY;
    private ItemStack weaponSlot = ItemStack.EMPTY;

    private String weaponType = "";
    private boolean isSheathed = true;
    private boolean isActive = false;

    private float moveSpeedPenalty = 0.0f;
    // HunterTransformation.java に追加
    public SkillId getEquippedSkill(HunterSkill.HunterSkillSlot slot) {
        return equippedSkills.get(slot);
    }
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

    public static boolean isHunter(Player player) {
        if (player == null) return false;
        return player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .map(HunterTransformation::isActive)
                .orElse(false);
    }

    private final Map<HunterSkill.HunterSkillSlot, SkillId> equippedSkills = new HashMap<>();

    public void setSkill(HunterSkill.HunterSkillSlot slot, SkillId skillId, Player player) {
        this.equippedSkills.put(slot, skillId);

        // サーバー側ならクライアントへ同期
        if (player instanceof ServerPlayer sp) {
            syncToClient(sp);
        }
    }


    // ================================================================
    // WeaponSlot（Inventory外）
    // ================================================================
    public void setWeaponSlotServer(ItemStack stack, ServerPlayer player) {
        if (!canAcceptWeapon(stack)) return;

        // 以前の武器カテゴリを保持（比較用）
        String oldType = this.weaponType;

        this.weaponSlot = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        this.weaponSlot.setCount(1);

        // ★ 武器種（String）を自動更新（WeaponCategoryUtilを使用）
        this.weaponType = com.mimic.monstermod.weapon.WeaponCategoryUtil.getCategory(this.weaponSlot).getId();

        // 武器が変わったならコンボをリセット
        if (!this.weaponType.equals(oldType)) {
            this.resetCombo();
        }

        syncToClient(player);
    }
    public HunterSkill getEquippedHunterSkill(HunterSkill.HunterSkillSlot slot) {
        SkillId id = equippedSkills.get(slot);
        if (id == null) return null;
        // RegistryなどからSkillIdに対応するHunterSkillインスタンスを返す
        return HunterSkillRegistry.get(id);
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

    public void startHunter(Player player) {
        isActive = true;

        if (this.identity == null) {
            BaseEntity renderEnt = null;
            if (player.level().isClientSide) {
                renderEnt = com.mimic.monstermod.init.ModEntitieType.HUNTER.get().create(player.level());
            }

            // 引数を (BaseEntity, int) に合わせる
            this.identity = new com.mimic.monstermod.identity.HunterIdentity(renderEnt, 4);
        }

        // Identityが存在していようがいまいが、現在の装備スキルで強制同期する
        if (this.identity instanceof HunterIdentity hi) {
            hi.forceRefreshSkills(player);
        }

        if (player instanceof ServerPlayer sp) syncToClient(sp);
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

    // 武器変更
    public void equipWeapon(Player player, ItemStack stack, String type) {
        this.equippedWeapon = stack.copy();
        this.weaponType = type;

        // isActive は変更しない

        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    // 納刀 / 抜刀
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

    // NBT
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("Sheathed", isSheathed);
        tag.putBoolean("IsActive", isActive);
        tag.putString("WeaponType", weaponType);
        tag.putInt("ComboCount", comboCount);
        tag.putFloat("AttackStiffness", attackStiffness);
        if (this.identity != null) {
            tag.put("IdentityData", this.identity.serializeNBT());
        }
        CompoundTag skillsTag = new CompoundTag();
        equippedSkills.forEach((slot, id) -> {
            skillsTag.putString(slot.name(), id.toString()); // "monstermod:test_skill" 形式
        });
        tag.put("EquippedSkills", skillsTag);

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

        // 1. 基本状態の読み込み
        isSheathed = tag.getBoolean("Sheathed");
        isActive = tag.getBoolean("IsActive");
        weaponType = tag.getString("WeaponType");
        comboCount = tag.getInt("ComboCount");
        attackStiffness = tag.getFloat("AttackStiffness");

        // 2. アイテム・装備の読み込み
        equippedWeapon = tag.contains("EquippedWeapon")
                ? ItemStack.of(tag.getCompound("EquippedWeapon"))
                : ItemStack.EMPTY;
        weaponSlot = tag.contains("WeaponSlot")
                ? ItemStack.of(tag.getCompound("WeaponSlot"))
                : ItemStack.EMPTY;

        // 3. スキルマッピングの復元
        equippedSkills.clear();
        if (tag.contains("EquippedSkills")) {
            CompoundTag skillsTag = tag.getCompound("EquippedSkills");
            for (String key : skillsTag.getAllKeys()) {
                try {
                    HunterSkill.HunterSkillSlot slot = HunterSkill.HunterSkillSlot.valueOf(key);
                    String[] split = skillsTag.getString(key).split(":");
                    equippedSkills.put(slot, SkillId.of(split[0], split[1]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 4. Identity の復元と同期
        if (tag.contains("IdentityData")) {
            if (this.identity == null) {
                this.identity = new com.mimic.monstermod.identity.HunterIdentity(null, 4);
            }
            this.identity.deserializeNBT(tag.getCompound("IdentityData"));

            // ★ ここで Map から配列へ強制同期を行う
            refreshIdentitySkills();
        }
    }

    // 確実にスキル配列を再構築するメソッド
    public void refreshIdentitySkills() {
        if (this.identity instanceof HunterIdentity hi) {
            hi.syncSkillsFromCapability(this.equippedSkills);
        }
    }

    // ================================================================
    // Client Load / Sync 後
    // ================================================================
    public void onLoad(Player player) {
        if (!isSheathed && isActive) {
            HunterUtil.disableHotbarRender(player);
        } else {
            HunterUtil.enableHotbarRender(player);
            HunterUtil.removeMovePenalty(player);
        }
    }
}
