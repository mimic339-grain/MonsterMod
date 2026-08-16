package com.mimic.monstermod.bomb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * ボムの起爆までの時間を決める場所。
 *
 * 【7:3 のランダム】
 * アイテムボムとブロックボムは、起動したときに
 *   7割 … 普通にタイマーが動き出す
 *   3割 … タイマーが0秒、つまりその場で即爆発
 * となる。何が起きるか分からないので、触る側は毎回賭けになる。
 *
 * 【時間そのもののランダム】
 * タイマーが動く場合の長さは既定で1〜5分のあいだからランダムに決まる。
 * コマンドで固定値に変えられる({@link BombStore#setFixedFuse})ので、
 * 「30秒固定でやろう」という遊び方もできる。
 */
public final class BombTiming {

    private BombTiming() {}

    public static final int TICKS_PER_SECOND = 20;

    /** 既定のタイマー範囲(1分〜5分) */
    public static final int MIN_FUSE_TICKS = 60 * TICKS_PER_SECOND;
    public static final int MAX_FUSE_TICKS = 300 * TICKS_PER_SECOND;

    /** タイマーが動く確率。残りは即爆発になる */
    private static final float TIMER_CHANCE = 0.7F;

    /** 即爆発とみなす残り時間。0にはできないので最短の1tick */
    public static final int INSTANT_TICKS = 1;

    /**
     * 起動時の残り時間を決める。
     * 3割の確率で即爆発になる。
     */
    public static int rollFuse(ServerLevel level) {
        RandomSource rng = level.getRandom();
        if (rng.nextFloat() > TIMER_CHANCE) return INSTANT_TICKS;
        return rollTimedFuse(level);
    }

    /** 必ずタイマーが動く場合の長さ。設置ボムなど、時間を自分で決めるもの以外に使う */
    public static int rollTimedFuse(ServerLevel level) {
        int fixed = BombStore.get(level).getFixedFuse();
        if (fixed > 0) return fixed;

        RandomSource rng = level.getRandom();
        return MIN_FUSE_TICKS + rng.nextInt(MAX_FUSE_TICKS - MIN_FUSE_TICKS + 1);
    }

    /**
     * 残り時間から爆発半径を決める(設置ボム用)。長く待つほど大きく爆発する。
     *
     * 即爆6 / 10秒12 / 30秒25 / 1分50 を基準にして、その間は直線でつないでいる。
     * 待つほど割に合うようにしてあるので、
     * 「すぐ起爆して仕留めるか、長く仕掛けて広く巻き込むか」の選択になる。
     */
    public static float radiusForFuse(int fuseTicks) {
        float seconds = fuseTicks / (float) TICKS_PER_SECOND;

        if (seconds <= 0.0F) return 6.0F;
        if (seconds <= 10.0F) return lerp(seconds, 0.0F, 10.0F, 6.0F, 12.0F);
        if (seconds <= 30.0F) return lerp(seconds, 10.0F, 30.0F, 12.0F, 25.0F);
        if (seconds <= 60.0F) return lerp(seconds, 30.0F, 60.0F, 25.0F, 50.0F);
        return 50.0F; // 1分より長く待っても、これ以上は大きくならない
    }

    private static float lerp(float v, float inMin, float inMax, float outMin, float outMax) {
        float t = (v - inMin) / (inMax - inMin);
        return outMin + (outMax - outMin) * t;
    }

    /** 残り時間を「1分23秒」のような表示にする */
    public static String format(int ticks) {
        int total = Math.max(0, ticks) / TICKS_PER_SECOND;
        int min = total / 60;
        int sec = total % 60;
        return min > 0 ? (min + "分" + sec + "秒") : (sec + "秒");
    }
}
