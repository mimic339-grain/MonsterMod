package com.mimic.monstermod.bomb;

/**
 * ボムの種類。
 *
 * どれも「対象に付与された状態」として同じ仕組みで扱う。
 * 付与を消せば爆発しない、という一点で統一されている。
 */
public enum BombKind {

    /** 殴って付与するボム。時間が来ると爆発し、付けられた本人は必ず死ぬ */
    TOUCH(true, true),

    /** アイテムに付与するボム。そのアイテムを右クリックすると爆発する */
    ITEM(true, true),

    /** ブロック・感圧板に付与するボム。踏むとタイマーが動き出す */
    BLOCK(true, true),

    /** 手持ちから設置する大型ボム。設置時間が長いほど爆発半径が大きい */
    PLACED(true, false),

    /** 受け渡し型。殴った相手へ移る。タイマーは引き継がれるので押し付け合いになる */
    RELAY(true, true),

    /**
     * 連鎖ボム。爆発したとき、範囲内の他のボムを即座に起爆させる。
     * 仕掛けを繋げておくと芋づる式に誘爆する
     */
    CHAIN(true, false),

    /**
     * ダミー(偽物)。音は本物と同じように鳴るが、爆発は小さく地形も壊さない。
     * 死なないが体力の半分ほどを持っていく。
     * 解除しても残骸は手に入らないので、解除キットの無駄遣いを誘える
     */
    DUMMY(false, false);

    /** 地形を壊すか */
    private final boolean breaksTerrain;
    /** 付けられた本人を必ず倒すか */
    private final boolean killsCarrier;

    BombKind(boolean breaksTerrain, boolean killsCarrier) {
        this.breaksTerrain = breaksTerrain;
        this.killsCarrier = killsCarrier;
    }

    public boolean breaksTerrain() { return breaksTerrain; }
    public boolean killsCarrier() { return killsCarrier; }

    /** 解除したときに残骸(素材)が手に入るか。偽物からは何も出ない */
    public boolean dropsRemnant() { return this != DUMMY; }

    public static BombKind byName(String name) {
        for (BombKind k : values()) {
            if (k.name().equals(name)) return k;
        }
        return TOUCH;
    }
}
