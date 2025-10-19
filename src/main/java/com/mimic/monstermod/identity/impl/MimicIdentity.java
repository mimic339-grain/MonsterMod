package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Mimic Identity 完全版（BaseMonsterIdentity対応）
 * - BoundingBox, EyeHeight, 能力処理, クライアント入力, NBT対応
 * - サーバーTickでスキル・クールタイム反映
 */
public class MimicIdentity extends BaseMonsterIdentity {

    public static final ResourceLocation IDENTITY_ID =
            new ResourceLocation("monstermod", "mimic");

    private static final int SKILL_COUNT = 3;

    /** Entity から作るコンストラクタ */
    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILL_COUNT);
    }

    /** IDだけで作る場合 */
    public MimicIdentity() {
        super(IDENTITY_ID.toString(), SKILL_COUNT);
    }

    // -----------------------------
    // BoundingBox / EyeHeight
    // -----------------------------
    @Override
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return 0.45f;
    }

    // -----------------------------
    // サーバー専用 Tick（能力・クールタイム更新）
    // -----------------------------
    public void tickServer(Player player) {
        super.tickServer(player); // abilityCooldowns 減算

        BaseMonsterEntity entity = getEntity();
        if (entity != null) {
            IMonsterData data = entity.getMonsterData();
            if (data != null) {
                // MonsterData にも反映可能
                data.setAbilityCooldown(Math.max(0, data.getAbilityCooldown() - 1));
                data.setRemainingHostilityTime(Math.max(0, data.getRemainingHostilityTime() - 1));
            }
        }
    }

    // -----------------------------
    // Identity固有能力 / メニュー処理
    // -----------------------------
    @Override
    protected void handleAbility(Player player, int skillIndex) {
        if (getCooldown(skillIndex) > 0) return;

        switch (skillIndex) {
            case 0 -> triggerAttack(player);
            case 1 -> deployTrap(player);
            case 2 -> someOtherSkill(player);
        }

        setCooldown(skillIndex, 20); // 1秒クール
    }

    @Override
    protected void handleMenu(Player player) {
        // Mimic専用GUIやメニュー処理
    }

    @Override
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    // -----------------------------
    // Mimic固有能力例
    // -----------------------------
    private void triggerAttack(Player player) {
        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;
        // TODO: 攻撃処理実装
    }

    private void deployTrap(Player player) {
        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;
        // TODO: トラップ設置処理実装
    }

    private void someOtherSkill(Player player) {
        BaseMonsterEntity entity = getEntity();
        if (entity == null) return;
        // TODO: 追加スキル処理
    }

    // -----------------------------
    // NBT保存 / 読み込み
    // -----------------------------
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        // Mimic固有ステートがあればここに保存
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        // Mimic固有ステート復元
    }
}
