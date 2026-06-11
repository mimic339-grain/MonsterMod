package com.mimic.monstermod.capability.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 全Capability共通の基底クラス。
 * EFM: api/capability/EntityCapability.java のパターンを参考に実装。
 *
 * 実装すべきメソッド:
 *   serializeNBT()   — 変身状態・HP・スキルをTagに保存
 *   deserializeNBT() — TagからCapability状態を復元
 *   syncToClient()   — 変更をS2Cブロードキャスト
 *   onPlayerRespawn()— 死亡後リスポーン時の状態引き継ぎ
 *
 * 配置: com/mimic/monstermod/capability/base/BaseTransformation.java
 */
public abstract class BaseTransformation {

    protected Player player;

    public void setPlayer(Player player) {
        this.player = player;
    }

    /** NBTに状態を保存 (PlayerEvent.SaveData から呼ぶ) */
    public abstract CompoundTag serializeNBT();

    /** NBTから状態を復元 (PlayerEvent.LoadData から呼ぶ) */
    public abstract void deserializeNBT(CompoundTag tag);

    /**
     * S2Cブロードキャスト — サーバー側で状態変更後に必ず呼ぶ。
     * EFMのCapability.updateEntityState() パターン。
     */
    public abstract void syncToClient();

    /**
     * 死亡後リスポーン時に旧Capabilityの内容を新Capabilityにコピー。
     * EFMの PlayerEvent.Clone パターン。
     * keepInventory=true の場合は完全コピー。
     */
    public void onPlayerRespawn(BaseTransformation old, boolean keepInventory) {
        if (keepInventory) {
            this.deserializeNBT(old.serializeNBT());
        } else {
            onDeathReset();
        }
    }

    /** 死亡時のリセット処理 (keepInventory=false の場合) */
    protected void onDeathReset() {
        // サブクラスでオーバーライドしてリセット処理を実装
    }
}