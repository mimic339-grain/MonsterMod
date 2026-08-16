package com.mimic.monstermod.identity.bomber;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.skill.SkillId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * ボマー。見た目は変えず、固定のスキルだけを与える役職。
 *
 * 【変身しない役職として作る理由】
 * スキルが固定である以上、既存の変身(Identity)の仕組みに載せるのが一番素直。
 * ただしモデル・スキン・体格・HP表示は一切変えたくないので
 * {@link #hasOwnBody()} を false にしている。
 * これだけでモンスター用HPバー・影消し・炎上表示消しが除外され、
 * 当たり判定や段差の自動上りは元から実体の有無で判定しているので勝手に外れる。
 * 人狼など今後の役職も同じやり方で追加できる。
 *
 * 【武装(armed)という状態】
 * 「殴った相手にボムを付ける」系のスキルは、押した瞬間には何も起きず
 * 次の一撃まで待つ必要がある。この待ち状態を armed として持つ。
 * HUDでは緑(使用可能)から青(武装中)に変わる({@link #isArmed(int)} をHUDが見る)。
 * 実際に付与するのは {@link BomberEvents}。
 */
public class BomberIdentity extends BaseIdentity {

    public static final String ID = "monstermod:bomber";

    /** スキルの並び。HUDの並びもこの順になる */
    public static final int SLOT_TOUCH = 0;   // 殴った相手にボムを付ける
    public static final int SLOT_ITEM = 1;    // 手に持っているアイテムにボムを仕込む
    public static final int SLOT_BLOCK = 2;   // ブロックにボムを仕掛ける
    public static final int SLOT_VANISH = 3;  // 透明化
    public static final int SLOT_PLACE = 4;   // 設置用のボムを手に入れる
    public static final int SLOT_RELAY = 5;   // 受け渡しボム
    public static final int SLOT_CHAIN = 6;   // 連鎖ボムを入手
    public static final int SLOT_DUMMY = 7;   // 偽ボムを入手

    private static final SkillId[] SKILLS = {
            BomberSkills.TOUCH,
            BomberSkills.ITEM,
            BomberSkills.BLOCK,
            BomberSkills.VANISH,
            BomberSkills.PLACE,
            BomberSkills.RELAY,
            BomberSkills.CHAIN,
            BomberSkills.DUMMY
    };

    /** クールダウン(tick)。仕掛け系は回転を速めに、透明化と設置ボムは重めにしてある */
    private static final int[] COOLDOWNS = { 120, 200, 160, 600, 400, 200, 500, 300 };

    /** 武装中のスキル。次の行動で消費される */
    private final boolean[] armed = new boolean[SKILLS.length];

    /** ブロックへ仕掛けている最中の進み具合(tick)。ゲージ表示に使う */
    private int plantProgress;

    public int getPlantProgress() { return plantProgress; }
    public void setPlantProgress(int ticks) { this.plantProgress = Math.max(0, ticks); }

    public BomberIdentity(@Nullable BaseEntity entity) {
        super(entity, SKILLS.length, ID);
        this.skillIds = SKILLS;
        this.defaultCooldowns = COOLDOWNS;
    }

    /** 見た目・体格・HP表示を一切変えない役職であることを示す */
    @Override
    public boolean hasOwnBody() {
        return false;
    }

    // ---------------- 武装状態 ----------------

    @Override
    public boolean isArmed(int index) {
        return index >= 0 && index < armed.length && armed[index];
    }

    public void setArmed(int index, boolean value) {
        if (index < 0 || index >= armed.length) return;
        armed[index] = value;
    }

    /** 武装していたら解除して true を返す。1回使い切りにするためのもの */
    public boolean consumeArmed(int index) {
        if (!isArmed(index)) return false;
        armed[index] = false;
        return true;
    }

    /** 殴る系の武装が同時に立たないよう、片方を立てるときはもう片方を下ろす */
    public void armExclusiveMelee(int index) {
        armed[SLOT_TOUCH] = false;
        armed[SLOT_RELAY] = false;
        setArmed(index, true);
    }

    /**
     * 武装状態をクライアントへ送る。
     *
     * 【毎回明示的に送る必要がある理由】
     * Identityの中身がクライアントへ流れるのは変身したときだけで、
     * スキルを押しただけでは同期されない。
     * 送らないとHUDの枠が緑のまま変わらず、武装しているかが本人にも分からない。
     */
    public static void sync(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        sp.getCapability(com.mimic.monstermod.variable.CapabilityRegistry.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> trans.syncToClient(sp));
    }

    // ---------------- 保存・同期 ----------------
    // 武装状態はHUDの色に直結するので、クライアントにも届く必要がある。
    // Identityのserialize/deserializeはCapabilityの同期にそのまま乗るため、ここに入れておく。

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        byte[] flags = new byte[armed.length];
        for (int i = 0; i < armed.length; i++) flags[i] = (byte) (armed[i] ? 1 : 0);
        tag.putByteArray("armed", flags);
        tag.putInt("plant", plantProgress);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        plantProgress = tag.getInt("plant");
        if (!tag.contains("armed")) return;
        byte[] flags = tag.getByteArray("armed");
        for (int i = 0; i < armed.length && i < flags.length; i++) armed[i] = flags[i] != 0;
    }

    // ---------------- 補助 ----------------

    /** そのプレイヤーがボマーなら Identity を返す。違えば null */
    @Nullable
    public static BomberIdentity of(Player player) {
        var cap = player.getCapability(
                com.mimic.monstermod.variable.CapabilityRegistry.PLAYER_TRANSFORMATION);
        if (!cap.isPresent()) return null;

        var trans = cap.resolve().orElse(null);
        if (trans == null || !trans.isTransformed()) return null;
        return trans.getIdentity() instanceof BomberIdentity bomber ? bomber : null;
    }

    /** 空でなく、既にボムが付いていないアイテムか(仕込める対象かの判定) */
    public static boolean canBombItem(ItemStack stack) {
        return !stack.isEmpty() && !com.mimic.monstermod.bomb.BombAttachment.has(stack);
    }
}
