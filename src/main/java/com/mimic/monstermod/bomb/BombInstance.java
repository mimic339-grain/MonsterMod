package com.mimic.monstermod.bomb;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * 仕掛けられたボム1個ぶんの状態。
 *
 * 【「付与」として扱う理由】
 * ボムは実体(エンティティ)ではなく、対象に付いた状態として持つ。
 * こうすると
 *   ・解除 = この付与を消すだけ
 *   ・受け渡し = この付与を別の対象へ移すだけ(タイマーはそのまま引き継がれる)
 *   ・2重掛け = 対象が複数の付与を持つだけ
 * と、要望の挙動が全部そのまま表現できる。
 *
 * 付ける先はエンティティ・アイテム・ブロックの3種類あるが、
 * 中身はどれもこのクラス1つで済むようにしてある({@link BombAttachment} 参照)。
 */
public class BombInstance {

    /** 1回の点滅音の最短間隔(tick)。これ以上は速くならない */
    private static final int MIN_BEEP_INTERVAL = 2;
    /** 点滅音の最長間隔(tick)。仕掛けた直後はこの間隔 */
    private static final int MAX_BEEP_INTERVAL = 20;

    private final BombKind kind;
    /** 仕掛けた人。爆発のダメージ元と、味方判定に使う */
    private final UUID owner;
    /** 残り時間(tick)。0になった瞬間に爆発する */
    private int fuseTicks;
    /** 仕掛けたときの時間。音の加速具合を出すのに使う */
    private final int totalTicks;
    /** 爆発の半径(ブロック) */
    private final float radius;
    /** タイマーが動いているか。踏むまで動かないブロックボムのために持つ */
    private boolean armed;

    public BombInstance(BombKind kind, UUID owner, int fuseTicks, float radius, boolean armed) {
        this.kind = kind;
        this.owner = owner;
        this.fuseTicks = Math.max(1, fuseTicks);
        this.totalTicks = this.fuseTicks;
        this.radius = radius;
        this.armed = armed;
    }

    private BombInstance(BombKind kind, UUID owner, int fuseTicks, int totalTicks,
                         float radius, boolean armed) {
        this.kind = kind;
        this.owner = owner;
        this.fuseTicks = fuseTicks;
        this.totalTicks = totalTicks;
        this.radius = radius;
        this.armed = armed;
    }

    public BombKind getKind() { return kind; }
    public UUID getOwner() { return owner; }
    public int getFuseTicks() { return fuseTicks; }
    public int getTotalTicks() { return totalTicks; }
    public float getRadius() { return radius; }
    public boolean isArmed() { return armed; }

    public void arm() { this.armed = true; }

    /** 残り時間を1減らす。0になったら true(=爆発)を返す */
    public boolean tickDown() {
        if (!armed) return false;
        fuseTicks--;
        return fuseTicks <= 0;
    }

    /** 即座に起爆させる(連鎖・火打ち石など) */
    public void detonateIn(int ticks) {
        this.armed = true;
        this.fuseTicks = Math.min(this.fuseTicks, Math.max(1, ticks));
    }

    /**
     * 今tickで点滅音を鳴らすか。
     * 残りが減るほど間隔が詰まっていくので、聞いているだけで切迫具合が分かる。
     */
    public boolean shouldBeep() {
        if (!armed) return false;
        float progress = 1.0F - (float) fuseTicks / Math.max(1, totalTicks);
        int interval = Math.round(Mth.lerp(progress, MAX_BEEP_INTERVAL, MIN_BEEP_INTERVAL));
        return fuseTicks % Math.max(1, interval) == 0;
    }

    /** 音の高さ。終盤ほど高くして焦りを煽る */
    public float beepPitch() {
        float progress = 1.0F - (float) fuseTicks / Math.max(1, totalTicks);
        return 0.8F + progress * 1.2F;
    }

    // ---- NBT ----
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", kind.name());
        if (owner != null) tag.putUUID("owner", owner);
        tag.putInt("fuse", fuseTicks);
        tag.putInt("total", totalTicks);
        tag.putFloat("radius", radius);
        tag.putBoolean("armed", armed);
        return tag;
    }

    public static BombInstance load(CompoundTag tag) {
        return new BombInstance(
                BombKind.byName(tag.getString("kind")),
                tag.hasUUID("owner") ? tag.getUUID("owner") : null,
                tag.getInt("fuse"),
                Math.max(1, tag.getInt("total")),
                tag.getFloat("radius"),
                tag.getBoolean("armed"));
    }

    /** 受け渡し用に、タイマーをそのまま保った複製を作る */
    public BombInstance copyForTransfer() {
        return new BombInstance(kind, owner, fuseTicks, totalTicks, radius, armed);
    }
}
