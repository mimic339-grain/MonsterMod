package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Mimic Identity 完全版
 * - BaseMonsterIdentity に準拠
 * - サーバー側 Tick は BaseMonsterIdentity に統合
 * - クライアント入力は BaseMonsterIdentity で統一
 */
public class MimicIdentity extends BaseMonsterIdentity {

    public static final ResourceLocation IDENTITY_ID =
            new ResourceLocation("monstermod", "mimic");

    private static final int SKILL_COUNT = 3;

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILL_COUNT);
    }

    public MimicIdentity() {
        super(null, SKILL_COUNT);
    }

    // -----------------------------
    // BoundingBox / EyeHeight
    // -----------------------------
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    public float getEyeHeight(Pose pose) {
        return 0.45f;
    }

    // -----------------------------
    // 能力処理（固有スキル）
    // -----------------------------
    @Override
    protected void handleAbility(Player player, int skillIndex) {
        if (abilityCooldowns[skillIndex] > 0) return;

        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;

        switch (skillIndex) {
            case 0 -> triggerAttack(player);
            case 1 -> deployTrap(player);
            case 2 -> someOtherSkill(player);
        }

        // クールタイムは BaseMonsterIdentity の handleAbility にも設定済み
        // MonsterData 側も必要なら自動反映させる
    }

    @Override
    protected void handleMenu(Player player) {
        // Mimic専用GUIやメニュー処理
    }

    // -----------------------------
    // Mimic固有能力
    // -----------------------------
    private void triggerAttack(Player player) {
        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;
        entity.performAbility(0);
    }

    private void deployTrap(Player player) {
        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;
        entity.performAbility(1);
    }

    private void someOtherSkill(Player player) {
        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;
        entity.performAbility(2);
    }

    // -----------------------------
    // NBT保存 / 復元
    // -----------------------------
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        // Mimic固有ステートがあればここに追加
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        // Mimic固有ステートがあればここから復元
    }
}
