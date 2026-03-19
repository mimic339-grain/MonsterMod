package com.mimic.monstermod.Math;

import net.minecraft.world.phys.Vec3;

/**
 * BlockMain
 *
 * 【役割】
 * ・Block 空間 → World(Vec3) への射影ルールを定義
 * ・MathMain を Block 基準で使う唯一の窓口
 *
 * 【設計原則】
 * ・形状ロジックを一切持たない
 * ・contains の数式を一切持たない
 * ・MathMain.contains() だけを信じる
 *
 * 【注意】
 * ・Block の「代表点」をどこに取るか、という
 *   座標系のルールのみを定義する
 */
public final class BlockMain {

    private BlockMain() {}

    /**
     * ブロック上面中央で AoE 判定
     * （2DBlockOverlay / 視覚用途向け）
     */
    public static boolean containsBlockTop(
            MathMain math,
            int bx, int by, int bz
    ) {
        Vec3 p = new Vec3(
                bx + 0.5,
                by + 1.0,
                bz + 0.5
        );
        return math.contains(p);
    }

    /**
     * ブロック中心点で AoE 判定
     * （2DBlockDiscrete / ロジック用途向け）
     */
    public static boolean containsBlockCenter(
            MathMain math,
            int bx, int by, int bz
    ) {
        Vec3 p = new Vec3(
                bx + 0.5,
                by + 0.5,
                bz + 0.5
        );
        return math.contains(p);
    }
}
