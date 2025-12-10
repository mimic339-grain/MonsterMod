package com.mimic.monstermod.capability;

import com.mimic.monstermod.weapon.WeaponCategory;
import com.mimic.monstermod.weapon.WeaponCategoryUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;


public class HunterCombatState {


    private boolean drawn = false;
    private int comboStep = 0;
    private int comboGraceTimer = 0;
    private int attackStiffness = 0;

    private WeaponCategory currentCategory = WeaponCategory.NONE;
    private float baseDamage = 1.0f;
    private float attackRange = 2.0f;
    private float moveSpeedMultiplier = 1.0f;
    private float hpBonus = 0f;

    private float[] damageMultiplier = new float[]{1.0f};
    private int[] stiffnessList = new int[]{10};
    private String[] animationList = new String[]{null};
    private int maxCombo = 1;

    private int comboGraceTicks = 12;
    private boolean cancelOnDodge = true;

    // ============================================================
// 基本
// ============================================================
    public boolean isDrawn() { return drawn; }
    public int getComboStep() { return comboStep; }
    public boolean isStiff() { return attackStiffness > 0; }

    public void setDrawn(boolean newState) {
        if (this.drawn == newState) return;
        this.drawn = newState;
        if (!newState) {
            resetCombo();
            attackStiffness = 0;
        }
    }

// ============================================================
// 攻撃ロジック
// ============================================================

    public boolean canAcceptAttack() {
        if (!drawn) return false;
        if (isStiff()) return false;
        return comboStep == 0 || comboGraceTimer > 0;
    }

    public AttackResult onAttackInput(Player player, ItemStack stack) {
        if (!canAcceptAttack()) return AttackResult.invalid();
        int stage = Math.min(comboStep, maxCombo - 1);
        float multiplier = damageMultiplier.length > stage ? damageMultiplier[stage] : damageMultiplier[0];
        float damage = computeBaseDamage(stack) * multiplier;
        int stiff = stiffnessList.length > stage ? stiffnessList[stage] : stiffnessList[stiffnessList.length - 1];
        String anim = animationList.length > stage ? animationList[stage] : null;
        advanceCombo();
        applyStiffness(stiff);
        return new AttackResult(true, stage, damage, stiff, anim);
    }

    public void advanceCombo() {
        comboStep = Math.min(comboStep + 1, maxCombo - 1);
        comboGraceTimer = comboGraceTicks;
    }

    public void resetCombo() {
        comboStep = 0;
        comboGraceTimer = 0;
    }

    public void applyStiffness(int ticks) {
        attackStiffness = Math.max(attackStiffness, ticks);
    }

    public void cancelForDodge() {
        if (!cancelOnDodge) return;
        resetCombo();
        attackStiffness = 0;
    }

// ============================================================
// カテゴリ関連
// ============================================================

    public void applyCategoryStats(WeaponCategory cat) {
        this.currentCategory = cat;
        this.baseDamage = cat.getBaseDamage();
        this.attackRange = cat.getAttackRange();
        this.moveSpeedMultiplier = cat.getMovePenalty();
        this.hpBonus = cat.getHpBonus();
        this.maxCombo = Math.max(1, cat.getComboMax());

        damageMultiplier = new float[maxCombo];
        for (int i = 0; i < maxCombo; i++) damageMultiplier[i] = 1.0f + i * 0.25f;

        stiffnessList = new int[maxCombo];
        for (int i = 0; i < maxCombo; i++) stiffnessList[i] = 10 + i * 8;

        animationList = new String[maxCombo];
        for (int i = 0; i < maxCombo; i++) animationList[i] = cat.getId() + "_attack_" + (i + 1);
    }

    /** 追加: 現在カテゴリを返す */
    public WeaponCategory getCurrentCategory() { return currentCategory; }

    /** 追加: 攻撃ダメージを返す（基礎値のみ） */
    public float getAttackDamage() { return baseDamage; }

    /** 追加: 武器Stackに基づいてカテゴリを更新 */
    public void updateCategory(ItemStack stack) {
        WeaponCategory cat = WeaponCategoryUtil.getCategory(stack);
        applyCategoryStats(cat);
    }

    // ============================================================
// Tick
// ============================================================
    public void tick(Player player) {
        if (attackStiffness > 0) attackStiffness--;
        if (comboGraceTimer > 0) {
            comboGraceTimer--;
            if (comboGraceTimer == 0) resetCombo();
        }
    }

    protected float computeBaseDamage(ItemStack stack) {
        WeaponCategory cat = WeaponCategoryUtil.getCategory(stack);
        return (cat != null ? cat.getBaseDamage() : baseDamage);
    }

    // ============================================================
// NBT
// ============================================================
    public CompoundTag serializeNBT() { /* 省略: 元のまま */ return new CompoundTag(); }
    public void deserializeNBT(CompoundTag tag) { /* 省略: 元のまま */ }

    public void syncToClient(ServerPlayer player) { /* 省略: 元のまま */ }

    // ============================================================
// AttackResult
// ============================================================
    public static class AttackResult {
        public final boolean valid;
        public final int stage;
        public final float damage;
        public final int stiffness;
        public final @Nullable String animation;

        public AttackResult(boolean valid, int stage, float damage, int stiffness, @Nullable String animation) {
            this.valid = valid;
            this.stage = stage;
            this.damage = damage;
            this.stiffness = stiffness;
            this.animation = animation;
        }

        public static AttackResult invalid() { return new AttackResult(false, -1, 0f, 0, null); }
    }


}
