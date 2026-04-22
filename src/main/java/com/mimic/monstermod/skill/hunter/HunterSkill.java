package com.mimic.monstermod.skill.hunter;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.EnumSet;

public abstract class HunterSkill {
    // 基礎データ
    public abstract SkillId getId();
    public abstract String getName();
    public abstract String getDescription();
    public abstract ResourceLocation getIcon();
    public abstract SkillType.Category getCategory();

    // 装備制限
    public EnumSet<HunterSkillSlot> getAllowedSlots() { return EnumSet.of(HunterSkillSlot.DODGE, HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3); }
    public abstract WeaponCategory getRequiredWeapon(); // nullなら全武器

    // 時間データ (Tick)
    public abstract int getCooldownTicks();
    public int getPreTicks() { return 0; }
    public int getActiveTicks() { return 1; }
    public int getPostTicks() { return 0; }
    // 後隙
    // ★ 追加：発動可能な抜刀状態
    public enum SheathState {
        SHEATHED_ONLY, // 納刀時のみ
        DRAWN_ONLY,    // 抜刀時のみ
        BOTH           // どちらでもOK
    }

    public abstract SheathState getAllowedState();
    // ★ ここに「実際の効果」を書く！
    public abstract void applyEffect(ServerPlayer player);

    public enum HunterSkillSlot {
        SKILL_1, SKILL_2, SKILL_3, DODGE
    }
    public com.mimic.monstermod.skill.SkillLead toLead() {
        return new com.mimic.monstermod.skill.SkillLead.Builder(getId())
                .category(getCategory())
                .attackType(com.mimic.monstermod.skill.SkillType.MOVEMENT)
                // プレビューの描画有無に関わらず、発動前の待機時間は PreviewTicks として扱う
                .totalPreviewTicks(getPreTicks())
                .beforeRecoverTicks(0) // ここは 0 で固定
                .effectTicks(getActiveTicks())
                .recoveryTicks(getPostTicks())
                .autoRoot(true)
                .canMoveDuringEffect(getCategory() == SkillType.Category.DASH)
                // hasPreview() が false なら描画フラグを立てないことで、
                // 「システム的な待ち時間は維持しつつ、見た目の円などは出さない」という動きになります
                .build();
    }
    public void onEffectEnd(ServerPlayer player) {
        // デフォルトは何もしない
    }
    public boolean canFitIn(HunterSkillSlot slot, WeaponCategory weapon) {
        boolean slotOk = getAllowedSlots().contains(slot);
        boolean weaponOk = (getRequiredWeapon() == null || getRequiredWeapon() == weapon);
        return slotOk && weaponOk;
    }
}