package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**

 * MimicIdentity 完全版
 * * BaseMonsterIdentity を継承
 * * internalOpen による open/close 状態管理
 * * skill 押下で open ↔ close を切り替える
 * * attack 削除
 * * Player attach 時に自動的に Entity を生成
 */
public class MimicIdentity extends BaseMonsterIdentity {

    private boolean internalOpen = false;
    private int pendingSkill = -1;
    @Nullable private Player attachedPlayer = null;

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(new ResourceLocation("monstermod", "mimic"), 1); // 能力スロット1
        setEntity(entity); // nullでもOK
    }

    /** ------------------------ Player attach ------------------------ */
    public void attachToPlayer(Player player) {
        this.attachedPlayer = player;
        // attach時に自動生成
        ensureClientEntity(player);
    }

    @Override
    @Nullable
    protected BaseMonsterEntity createClientEntity(Player player) {
        // クライアント専用 MimicEntity を生成（正しい EntityType を渡す）
        MimicEntity mimic = new MimicEntity(ModEntitieType.MIMIC.get(), player.level());
        mimic.setPos(player.getX(), player.getY(), player.getZ());
        return mimic;
    }

    /** ------------------------ Animation State 更新 ------------------------ */
    @Override
    protected void updateAnimationStateServer(Player player) {
        boolean isMoving = player.getDeltaMovement().lengthSqr() > 0.01;


        int skillPressed = consumeSkill();
        if (skillPressed >= 0) {
            internalOpen = !internalOpen;

            // トグルアニメーション再生
            String toggleAnim = internalOpen ? "open" : "close";
            playAnimation(toggleAnim, false, getAnimationTime(), 0.1f);
            return;
        }

        String next;
        boolean nextLoop = true;
        if (isMoving) {
            next = internalOpen ? "open_walk" : "close_walk";
        } else {
            next = internalOpen ? "open_idle" : "close_idle";
        }

        if (!next.equals(currentState) || loop != nextLoop) {
            playAnimation(next, nextLoop, getAnimationTime(), 0.1f);
        }

    }

    /** ------------------------ 入力管理 ------------------------ */
    public void setPendingSkill(int skillIndex) { this.pendingSkill = skillIndex; }
    @Override
    public int consumeSkill() { int val = pendingSkill; pendingSkill = -1; return val; }

    /** ------------------------ Entity 状態参照 ------------------------ */
    private boolean isEntityOpen() {
        if (entity instanceof MimicEntity mimic) {
            return mimic.isOpen();
        }
        return internalOpen;
    }

    /** ------------------------ 外部参照用 ------------------------ */
    public boolean getInternalOpen() { return internalOpen; }

    /** ------------------------ Tick オーバーライド ------------------------ */
    @Override
    public void tick(Player player, float deltaSeconds) {
        if (attachedPlayer == null) attachToPlayer(player);
        super.tick(player, deltaSeconds);
    }

    /** ------------------------ NBT 保存/復元 ------------------------ */
    @Override
    public void deserializeNBT(net.minecraft.nbt.CompoundTag tag) {
        super.deserializeNBT(tag);
        internalOpen = tag.getBoolean("internalOpen");
    }

    @Override
    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag tag = super.serializeNBT();
        tag.putBoolean("internalOpen", internalOpen);
        return tag;
    }
}
