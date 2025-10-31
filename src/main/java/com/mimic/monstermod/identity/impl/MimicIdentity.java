package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MimicIdentity 完全版
 * - BaseMonsterIdentity を継承
 * - internalOpen による open/close 状態管理
 * - skill 押下で open ↔ close を切り替える
 * - attack 削除
 */
public class MimicIdentity extends BaseMonsterIdentity {

    private boolean internalOpen = false; // internal 状態
    private int pendingSkill = -1;         // skill 押下フラグ

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(new ResourceLocation("monstermod", "mimic"), 1); // 能力スロット数は 1 で十分
        setEntity(entity); // nullでもOK
    }

    /**------------------------
     * Animation State 更新
     *------------------------*/
    @Override
    protected void updateAnimationStateServer(Player player) {
        boolean isMoving = player.getDeltaMovement().lengthSqr() > 0.01;

        // skill 押下で internalOpen をトグル
        int skillPressed = consumeSkill();
        if (skillPressed >= 0) {
            internalOpen = !internalOpen;

            // 押下時のアニメーション再生
            String toggleAnim = internalOpen ? "open" : "close";
            playAnimation(toggleAnim, false, getAnimationTime(), 0.1f); // loop = false
            return; // この tick はトグルアニメーションのみ
        }

        // 移動/待機アニメーション判定
        String next;
        boolean nextLoop = true;
        if (isMoving) {
            next = internalOpen ? "open_walk" : "close_walk";
        } else {
            next = internalOpen ? "open_idle" : "close_walk";
        }

        if (!next.equals(currentState) || loop != nextLoop) {
            playAnimation(next, nextLoop, getAnimationTime(), 0.1f);
        }
    }

    /**------------------------
     * 入力管理
     *------------------------*/
    public void setPendingSkill(int skillIndex) { this.pendingSkill = skillIndex; }
    public int consumeSkill() { int val = pendingSkill; pendingSkill = -1; return val; }

    /**------------------------
     * Entity 状態参照（安全に取得）
     *------------------------*/
    private boolean isEntityOpen() {
        if (entity instanceof MimicEntity mimic) {
            return mimic.isOpen();
        }
        return internalOpen; // Entity が null の場合は internalOpen を返す
    }

    /**------------------------
     * 外部参照用
     *------------------------*/
    public boolean getInternalOpen() { return internalOpen; }
}
