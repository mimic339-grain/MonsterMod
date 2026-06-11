package com.mimic.monstermod.model.anim;

/**
 * エンティティのモーション状態定義。
 * EFM: api/animation/LivingMotion.java / LivingMotions.java パターン。
 *
 * 各 LivingMotion は対応するアニメーション名（Blender JSONのaction名）を持つ。
 * LivingMotionManager が現在のMinecraft状態を監視して自動切り替えする。
 *
 * 使い方:
 *   entity.setAnimation(LivingMotion.WALK.animName)
 *   → AnimationPlayer.setPlayAnimation("walk")
 *
 * 配置: com/mimic/monstermod/model/anim/LivingMotion.java
 */
public enum LivingMotion {

    // ── 基本モーション ────────────────────────────────────────────────
    IDLE     ("idle",      true,  1.0f),   // 待機
    WALK     ("walk",      true,  1.0f),   // 歩行
    RUN      ("run",       true,  1.0f),   // 走行
    SNEAK    ("sneak",     true,  0.8f),   // しゃがみ
    SWIM     ("swim",      true,  1.0f),   // 水泳
    FLY      ("fly",       true,  1.0f),   // 飛行

    // ── 戦闘モーション ────────────────────────────────────────────────
    ATTACK   ("attack",    false, 1.0f),   // 通常攻撃
    HURT     ("hurt",      false, 1.2f),   // ダメージ
    DEATH    ("death",     false, 0.8f),   // 死亡
    KNOCKDOWN("knockdown", false, 1.0f),   // ノックダウン

    // ── Monster固有 ───────────────────────────────────────────────────
    BITE     ("bite",      false, 1.1f),   // 噛みつき
    ROAR     ("roar",      false, 0.9f),   // 咆哮
    BREATH   ("breath",    false, 1.0f),   // ブレス攻撃
    FLY_ATTACK("fly_attack",false, 1.0f),  // 飛行攻撃
    CHAIN    ("chain",     true,  0.5f),   // Chainデバフ（拘束状態）

    // ── Hunter固有 ────────────────────────────────────────────────────
    SKILL    ("skill",     false, 1.0f),   // スキル発動
    DODGE    ("dodge",     false, 1.3f),   // 回避
    GUARD    ("guard",     true,  1.0f);   // ガード

    public final String animName;   // Blender JSONのaction名
    public final boolean isLoop;    // ループ再生か
    public final float defaultSpeed; // デフォルト再生速度

    LivingMotion(String animName, boolean isLoop, float defaultSpeed) {
        this.animName     = animName;
        this.isLoop       = isLoop;
        this.defaultSpeed = defaultSpeed;
    }
}