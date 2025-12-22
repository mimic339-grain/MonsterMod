
package com.mimic.monstermod.animation;

/**
 * アニメーションの上書き（Override）判定ロジックを定義するクラス。
 * true: 既存のアニメーションを上書き（中断）して新しいものを再生する。
 * false: 既存のアニメーションが終了するまで待機する。（連続する動きや重要なスキルに適用）
 */
public class Animations {
    public static boolean ActiveAniamtion(String animation) {

        // 💡 デフォルト設定: false (上書きを禁止)
        boolean allowOverride = false;
        // 💡 JSONに定義されている6つのアニメーションに対して、allowOverride = false の状態を維持
        if (animation.equals("sword_simple_sheathed") ||
                animation.equals("sword_simple_idle") ||
                animation.equals("sword_simple_attack1") ||
                animation.equals("sword_simple_attack2")) {
        }
        /*
        if (animation.equals("player_animation/hammer_attack")) {
            allowOverride = true; // 例外: この攻撃は即座に再生されるべきなので、他のアイドルを上書きします
        }
        */

        return allowOverride;
    }
}
