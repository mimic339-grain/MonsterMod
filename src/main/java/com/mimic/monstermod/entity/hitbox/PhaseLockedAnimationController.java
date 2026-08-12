package com.mimic.monstermod.entity.hitbox;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;

/**
 * 再生位相をこちら側から指定できるAnimationController。
 *
 * 【なぜ必要か】
 * GeckoLibは AnimationController 内部の tickOffset を「自分がそのアニメーションを
 * 再生し始めた瞬間」に設定し、以降 adjustedTick = getTick() - tickOffset で
 * 再生位置を決める(AnimationController#adjustTick)。
 * つまり再生位相は「そのクライアントが再生を始めた実時刻」に依存するため、
 * ネットワーク遅延でサーバーとクライアントが別々の位相で回ってしまい、
 * サーバーが計算する当たり判定と、画面に見えているモデルの姿勢がズレる。
 *
 * ここで tickOffset を明示的に上書きし、両サイドが共有する絶対時刻
 * (level.getGameTime()) から導いた位相に強制的に合わせることで、
 * 遅延があっても位相が一致するようにする。
 *
 * tickOffset は protected なのでサブクラスからのみ触れる。
 */
public class PhaseLockedAnimationController<T extends GeoAnimatable> extends AnimationController<T> {

    public PhaseLockedAnimationController(T animatable, String name, int transitionTickTime,
                                          AnimationStateHandler<T> animationHandler) {
        super(animatable, name, transitionTickTime, animationHandler);
    }

    /**
     * 再生位相を強制する。
     *
     * @param currentTick   GeckoLibが今参照している時刻(= animatable.getTick(...) と同じ値)
     * @param desiredPhase  その時刻で再生されていてほしい位置(tick単位、0以上)
     */
    public void forcePhase(double currentTick, double desiredPhase) {
        this.tickOffset = currentTick - desiredPhase;
    }
}
