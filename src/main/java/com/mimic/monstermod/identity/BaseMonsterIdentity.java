package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.client.C2SPlayerInputPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 完全版: BaseMonsterIdentity
 * - クールタイム管理をサーバー側で統合
 * - プレイヤー入力をサーバーへ送信し、能力発動を管理
 * - クライアント側描画・アニメーションも更新
 */
public class BaseMonsterIdentity {

    protected final String id;
    @Nullable
    protected final BaseMonsterEntity entity;
    protected int[] abilityCooldowns;

    public BaseMonsterIdentity(@Nullable BaseMonsterEntity entity, int abilityCount) {
        this.entity = entity;
        this.abilityCooldowns = new int[abilityCount];
        this.id = entity != null ? entity.getType().toString() : "unknown";
    }

    public String getId() { return id; }
    @Nullable public BaseMonsterEntity getEntity() { return entity; }

    /** サーバーTick: クールダウンを減少させる */
    public void tickServer() {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;
        }
    }

    /** クライアント用: プレイヤー姿勢・装備同期 */
    public void copyFromPlayerClient(Player player) {
        if (entity == null) return;

        entity.setYRot(player.getYRot());
        entity.setXRot(player.getXRot());
        entity.setYHeadRot(player.getYHeadRot());
        entity.yRotO = player.yRotO;
        entity.xRotO = player.xRotO;
        entity.yHeadRotO = player.yHeadRotO;
        entity.setPose(player.getPose());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            entity.setItemSlot(slot, player.getItemBySlot(slot));
        }
    }

    /** クライアント入力（Identityが受けてEntityに渡す） */
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (entity == null) return;

        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    /** 能力発動処理（クールタイムチェック含む） */
    protected void handleAbility(Player player, int skillIndex) {
        if (abilityCooldowns[skillIndex] > 0) return;
        if (entity != null) {
            entity.performAbility(skillIndex);
            // クールタイムはここで設定
            int cd = entity.getMonsterData() != null ?
                    entity.getMonsterData().getSkillCooldown(skillIndex) : 20;
            abilityCooldowns[skillIndex] = cd;
        }
    }

    protected void handleMenu(Player player) {
        // 必要に応じてオーバーライド
    }

    // -----------------------------
    // クールタイム用Getter / Setter
    // -----------------------------
    public int[] getAbilityCooldowns() {
        return abilityCooldowns;
    }

    public void setAbilityCooldown(int index, int cd) {
        if (index < 0 || index >= abilityCooldowns.length) return;
        abilityCooldowns[index] = cd;
    }

    // -----------------------------
    // NBT保存・復元
    // -----------------------------
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        for (int i = 0; i < abilityCooldowns.length; i++) {
            tag.putInt("cd_" + i, abilityCooldowns[i]);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (tag.contains("cd_" + i)) abilityCooldowns[i] = tag.getInt("cd_" + i);
        }
    }

    // -----------------------------
    // サーバー入力ハンドル例（C2SPlayerInputPacket統合版）
    // -----------------------------
    public void handleServerInput(Player player, C2SPlayerInputPacket pkt) {
        if (entity == null) return;

        // 移動入力
        if (pkt.getForward() != 0 || pkt.getStrafe() != 0) entity.moveRelative(pkt.getForward(), pkt.getStrafe());
        if (pkt.isJump()) entity.jumpFromGround();
        entity.setSprinting(pkt.isSprint());
        entity.setPlayerActiveMove(pkt.getForward() != 0 || pkt.getStrafe() != 0 || pkt.isJump());

        // スキル入力
        if (pkt.isUseSkill() && pkt.getSkillIndex() >= 0) {
            if (abilityCooldowns[pkt.getSkillIndex()] <= 0) {
                entity.performAbility(pkt.getSkillIndex());
                int cd = entity.getMonsterData() != null ?
                        entity.getMonsterData().getSkillCooldown(pkt.getSkillIndex()) : 20;
                abilityCooldowns[pkt.getSkillIndex()] = cd;
            }
        }

        // メニュー入力
        if (pkt.isMenuOpen()) handleClientInput(player, false, true, -1);
    }
}
