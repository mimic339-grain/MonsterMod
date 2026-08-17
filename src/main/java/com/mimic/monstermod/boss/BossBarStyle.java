package com.mimic.monstermod.boss;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.resources.ResourceLocation;

/**
 * ボスバーの枠デザイン。
 *
 * Identity ごとに {@link com.mimic.monstermod.identity.BaseIdentity#getBossBarStyle()} で選ぶ。
 * 今は彫金フレーム1種だけだが、役職ごとに枠を変えられる作りは残してある。
 * 増やすときは値を足してテクスチャを同じ寸法で用意すればよい。
 *
 * 【テクスチャの作り】
 * 1枚 256x16 で、上下に2つの役割を詰め込んでいる。
 *   y  0〜7  : 枠そのもの。ゲージが減ったときに見える暗い溝も含む
 *   y  8〜15 : ゲージの中身(赤のグラデーション)。残量に応じて左から切り出して貼る
 * 高さはバニラのボスバーより細い。太いと画面上部を占有しすぎるため。
 *
 * 【ゲージの一番下を黒く落としていない理由】
 * 濃淡を付けようと下端を黒に近づけると、暗い溝と枠の影に溶けて
 * 「どこまでゲージが残っているか」が読めなくなる。
 * 下端も赤のまま留め、溝の側をわずかに明るい灰色にして分離している。
 *
 * 描画は {@link com.mimic.monstermod.client.BossBarRenderer}。
 * 生成スクリプトは開発時のみ使う PowerShell(リポジトリには含めていない)。
 */
public enum BossBarStyle {

    /** 彫金フレーム。両端に矢尻と小さな棘。どの役職にも合うよう中立的なモチーフにしてある */
    ORNATE("frame_ornate");

    // --- テクスチャ内の座標。描画側と生成スクリプトで必ず一致させること ---

    /** テクスチャ全体の大きさ */
    public static final int TEX_W = 256, TEX_H = 16;
    /** 枠の大きさ(画面にはこの大きさでそのまま貼る) */
    public static final int FRAME_W = 256, FRAME_H = 8;
    /** 枠の中でゲージが入る位置と大きさ */
    public static final int BAR_X = 34, BAR_Y = 2, BAR_W = 188, BAR_H = 4;
    /** ゲージの中身がテクスチャのどこにあるか(縦位置だけ枠と違う) */
    public static final int BAR_V = 10;

    private final ResourceLocation texture;

    BossBarStyle(String fileName) {
        this.texture = new ResourceLocation(MonsterMod.MOD_ID, "textures/gui/bossbar/" + fileName + ".png");
    }

    public ResourceLocation texture() {
        return texture;
    }

    /**
     * パケットで送る用の番号から復元する。
     * 知らない番号が来ても落ちないよう ORNATE に倒す。
     * 呼び出し元: {@link com.mimic.monstermod.network.server.S2C_BossBarStylePacket}
     */
    public static BossBarStyle byId(int id) {
        BossBarStyle[] all = values();
        if (id < 0 || id >= all.length) return ORNATE;
        return all[id];
    }
}
