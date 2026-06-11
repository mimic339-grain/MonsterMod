package com.mimic.monstermod.model.anim;

/**
 * EFMのAnimationPlayer.javaを参考にしたアニメーション再生エンジン。
 *
 * EFM参考:
 *   - api/animation/AnimationPlayer.java
 *   - tick() / getCurrentPose() / setPlayAnimation() / reset()
 *
 * 再生フロー:
 *   1. setPlayAnimation(animName) でアニメーションをセット
 *   2. tick() を毎tickで呼ぶ (CommonEvents / CustomEntityBase.tick())
 *   3. getCurrentPose(partialTick) で補間済み時刻を取得
 *   4. SkeletonPose.update(animation, animTime) でスキニング行列を更新
 *
 * 配置: com/mimic/monstermod/model/anim/AnimationPlayer.java
 */
public class AnimationPlayer {

    private static final float A_TICK = 1.0f / 20.0f; // EFM: EpicFightSharedConstants.A_TICK

    private float elapsedTime     = 0f;
    private float prevElapsedTime = 0f;
    private boolean isEnd         = false;
    private boolean isReversed    = false;
    private boolean doNotResetTime = false;

    /** 現在再生中のアニメーション名 */
    private String currentAnimName = "idle";
    /** 再生速度倍率 (1.0 = 通常速度) */
    private float playbackSpeed = 1.0f;
    /** ループ再生か */
    private boolean isLoop = true;

    // ── 再生制御 ─────────────────────────────────────────────────────

    /**
     * アニメーションをセット。EFM: setPlayAnimation() パターン。
     */
    public void setPlayAnimation(String animName, boolean loop) {
        if (doNotResetTime) {
            doNotResetTime = false;
            isEnd = false;
        } else {
            reset();
        }
        this.currentAnimName = animName;
        this.isLoop = loop;
    }

    public void setPlayAnimation(String animName) {
        setPlayAnimation(animName, true);
    }

    public void reset() {
        elapsedTime     = 0f;
        prevElapsedTime = 0f;
        isEnd           = false;
    }

    // ── 毎tick更新 ────────────────────────────────────────────────────

    /**
     * 毎tickで呼ぶ。EFM: AnimationPlayer.tick(entitypatch) パターン。
     * CustomEntityBase.tick() または LivingMotionManager.tick() から呼ぶ。
     *
     * @param totalTime アニメーションの総時間（秒）
     */
    public void tick(float totalTime) {
        prevElapsedTime = elapsedTime;
        float delta = A_TICK * playbackSpeed * (isReversed ? -1f : 1f);
        elapsedTime += delta;

        if (elapsedTime > totalTime) {
            if (isLoop) {
                prevElapsedTime = prevElapsedTime - totalTime;
                elapsedTime     = elapsedTime % totalTime;
            } else {
                elapsedTime = totalTime;
                isEnd = true;
            }
        } else if (elapsedTime < 0) {
            if (isLoop) {
                prevElapsedTime = totalTime - elapsedTime;
                elapsedTime     = totalTime + elapsedTime;
            } else {
                elapsedTime = 0f;
                isEnd = true;
            }
        }
    }

    /**
     * 補間済みアニメーション時刻を返す。
     * EFM: AnimationPlayer.getCurrentPose(partialTicks) の時刻計算部分。
     *
     * @param partialTick 0.0〜1.0のtick補間係数
     * @return 補間済み時刻（秒）
     */
    public float getInterpolatedTime(float partialTick) {
        return prevElapsedTime + (elapsedTime - prevElapsedTime) * partialTick;
    }

    // ── ブレンド ──────────────────────────────────────────────────────

    /**
     * 別アニメーションへのブレンド遷移用の補間係数を計算。
     * EFM: DynamicAnimation.getBlendWeight() パターン。
     *
     * @param blendDuration ブレンド時間（秒）
     * @return 0.0(前のアニメーション) 〜 1.0(新しいアニメーション)
     */
    public float getBlendWeight(float blendDuration) {
        if (blendDuration <= 0) return 1f;
        return Math.min(1f, elapsedTime / blendDuration);
    }

    // ── Getter/Setter ─────────────────────────────────────────────────
    public String  getCurrentAnimName()  { return currentAnimName; }
    public float   getElapsedTime()      { return elapsedTime; }
    public float   getPrevElapsedTime()  { return prevElapsedTime; }
    public boolean isEnd()               { return isEnd; }
    public boolean isLoop()              { return isLoop; }
    public void    setPlaybackSpeed(float s) { this.playbackSpeed = s; }
    public void    setReversed(boolean r)    { this.isReversed = r; }
    public void    markDoNotResetTime()      { this.doNotResetTime = true; }
}