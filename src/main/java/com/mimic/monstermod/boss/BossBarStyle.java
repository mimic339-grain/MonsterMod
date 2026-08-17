package com.mimic.monstermod.boss;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.resources.ResourceLocation;

/**
 * ボスバーの枠デザイン。
 *
 * Identity ごとに {@link com.mimic.monstermod.identity.BaseIdentity#getBossBarStyle()} で選ぶ。
 * どの役職にも合うよう、動物や属性を思わせないモチーフだけを用意している。
 *
 * 【テクスチャの作り】
 * 1枚 256x64 で、上下に2つの役割を詰め込んでいる。
 *   y  0〜31 : 枠そのもの。ゲージが減ったときに見える濃い赤の溝も含む
 *   y 32〜63 : ゲージの中身(青のグラデーション)。残量に応じて左から切り出して貼る
 * 描画は {@link com.mimic.monstermod.client.BossBarRenderer}。
 * 生成スクリプトは開発時のみ使う PowerShell(リポジトリには含めていない)。
 */
public enum BossBarStyle {

    /** 彫金フレーム。矢尻とらせん飾り。八咫烏など「格の高いボス」向け */
    ORNATE("frame_ornate"),
    /** 角ばった鋼のプレート。鋲だけの控えめな枠。視認性が一番高い */
    STEEL("frame_steel"),
    /** 牙モチーフ。内向きの牙が伸びる。獣系のボス向け */
    FANG("frame_fang");

    // --- テクスチャ内の座標。描画側と生成スクリプトで必ず一致させること ---

    /** テクスチャ全体の大きさ */
    public static final int TEX_W = 256, TEX_H = 64;
    /** 枠の大きさ(画面にはこの大きさでそのまま貼る) */
    public static final int FRAME_W = 256, FRAME_H = 32;
    /** 枠の中でゲージが入る位置と大きさ */
    public static final int BAR_X = 34, BAR_Y = 12, BAR_W = 188, BAR_H = 8;
    /** ゲージの中身がテクスチャのどこにあるか(縦位置だけ枠と違う) */
    public static final int BAR_V = 44;

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
